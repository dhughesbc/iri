package com.iota.iri;

import com.iota.iri.conf.Configuration;
import com.iota.iri.conf.Configuration.DefaultConfSettings;
import com.iota.iri.service.CallableRequest;
import com.iota.iri.service.dto.AbstractResponse;
import com.iota.iri.service.dto.ErrorResponse;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.sun.jmx.mbeanserver.Util.cast;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public class IXI {
    private static final Logger log = LoggerFactory.getLogger(IXI.class);

    private final ScriptEngine scriptEngine = (new ScriptEngineManager()).getEngineByName("JavaScript");
    /*
    private static final ScriptEngine scriptEngine = (new NashornScriptEngineFactory()).getScriptEngine((classname) ->
            !"com.iota.iri.IXI".equals(classname));
    */
    private final Map<String, Map<String, CallableRequest<AbstractResponse>>> ixiAPI = new HashMap<>();
    private final Map<String, Map<String, Runnable>> ixiLifetime = new HashMap<>();
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private WatchService watcher;
    private Thread dirWatchThread;

    /*
    TODO: get configuration variable for directory to watch
    TODO: initialize directory listener
    TODO: create events for target added/changed/removed
     */
    public void init() throws Exception {
        if(Configuration.string(DefaultConfSettings.IXI_DIR).length() > 0) {
            watcher = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(Configuration.string(DefaultConfSettings.IXI_DIR));
            String s = path.toAbsolutePath().toString();
            final File ixiDir = new File(s);
            if(!ixiDir.exists()) ixiDir.mkdir();
            register(path);
            dirWatchThread = (new Thread(this::processEvents));
            dirWatchThread.start();
        }
    }

    public void shutdown() throws InterruptedException {
        if(dirWatchThread != null) {
            dirWatchThread.interrupt();
            dirWatchThread.join(6000);
            Object[] keys = ixiAPI.keySet().toArray();
            for (Object key : keys) {
                detach((String)key);
            }
        }
    }

    private  void register (Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        watchKeys.put(key, dir);
        // TODO: Add existing files
        addFiles(dir);
    }

    private void addFiles (Path dir) throws IOException {
        Files.walk(dir).forEach(filePath -> {
            if(!filePath.equals(dir) && !filePath.toFile().isHidden())
                if(Files.isDirectory(filePath, NOFOLLOW_LINKS)) {
                    try {
                        log.info("Searching: "+ filePath);
                        addFiles(filePath);
                        register(filePath);
                    } catch (IOException e) {
                        log.error("Error adding Files.", e);
                    }
                } else {
                    log.info("File found: " + filePath.toString());
                    try {
                        attach(new FileReader(filePath.toFile()), filePath.getFileName().toString().replaceFirst("[.][^.]+$", ""));
                        //register(filePath);
                    } catch (ScriptException e) {
                        log.debug("Script exception: ", e);
                    } catch (IOException e) {
                        log.debug("Could not register path: ", e);
                    }
                }
        });
    }

    public AbstractResponse processCommand(final String command, Map<String, Object> request) {
        Map<String, CallableRequest<AbstractResponse>> ixiMap;
        AbstractResponse res;
        String substring;
        for (String key :
                ixiAPI.keySet()) {
            substring = command.substring(0, key.length());
            if(substring.equals(key)) {
                String subCmd = command.substring(key.length()+1);
                ixiMap = ixiAPI.get(key);
                res = ixiMap.get(subCmd).call(request);
                if(res != null) return res;
            }
        }
        return null;
    }

    private void processEvents() {
        while(!Thread.interrupted()) {
            synchronized(instance) {
                WatchKey key;
                try {
                    key = watcher.take();
                    pollEvents(key, watchKeys.get(key));
                } catch (InterruptedException e) {
                    log.error("Watcher interrupted: ", e);
                }
            }
        }
    }

    private void pollEvents(WatchKey key, Path dir) {
        for (WatchEvent<?> event: key.pollEvents()) {
            WatchEvent.Kind kind = event.kind();

            if(kind == OVERFLOW) {
                continue;
            }

            WatchEvent<Path> ev = cast(event);
            Path name = ev.context();
            Path child = dir.resolve(name);

            try {
                executeEvents(kind, child);
            } catch (IOException e) {
                log.debug("Could not load file.");
            } catch (ScriptException e) {
                log.debug("Could not load script.");
            }

            if (!key.reset()) {
                watchKeys.remove(key);
                if (watchKeys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void executeEvents(WatchEvent.Kind kind, Path child) throws IOException, ScriptException {
        if (kind == ENTRY_MODIFY || kind == ENTRY_DELETE) {

            log.debug("detach child: "+ child);
            detach(child.toString().replaceFirst("[.][^.]+$", ""));
        }
        if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                register(child);
            } else {
                //Files.isRegularFile(child)
                log.debug("Attempting to load "+ child);
                attach(new FileReader(child.toFile()), child.getFileName().toString().replaceFirst("[.][^.]+$", ""));
                log.debug("Done.");
            }
        }
    }

    private void attach(final Reader ixi, final String filename) throws ScriptException {
            Map<String, CallableRequest<AbstractResponse>> ixiMap = new HashMap<>();
            Map<String, Runnable> startStop = new HashMap<>();
            Bindings bindings = scriptEngine.createBindings();

            bindings.put("API", ixiMap);
            bindings.put("IXICycle", startStop);
            ixiAPI.put(filename, ixiMap);
            ixiLifetime.put(filename, startStop);
            scriptEngine.eval(ixi, bindings);
    }

    private void detach(String fileName) {
        Map<String, Runnable> ixiMap = ixiLifetime.get(fileName);
        if(ixiMap != null) {
            Runnable stop = ixiMap.get("shutdown");
            if (stop != null) stop.run();
        }
        ixiAPI.remove(fileName);
        ixiLifetime.remove(fileName);
    }

    private static final IXI instance = new IXI();

    public static IXI instance() {
        return instance;
    }

}
