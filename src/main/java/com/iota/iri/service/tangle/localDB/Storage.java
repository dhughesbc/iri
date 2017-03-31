package com.iota.iri.service.tangle.localDB;

import java.io.IOException;
import java.util.Arrays;

import com.iota.iri.model.Hash;
import com.iota.iri.model.Transaction;
import com.iota.iri.service.tangle.IPersistenceProvider;
import com.iota.iri.service.viewModels.TransactionViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage is organized as 243-value tree
 */
public class Storage extends AbstractStorage implements IPersistenceProvider {
	
    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    public static final byte[][] approvedTransactionsToStore = new byte[2][];

    private volatile boolean launched;

    public static int numberOfApprovedTransactionsToStore;

    private StorageTransactions storageTransactionInstance = StorageTransactions.instance();
    private StorageBundle storageBundleInstance = StorageBundle.instance();
    private StorageAddresses storageAddressesInstance = StorageAddresses.instance();
    private StorageTags storageTags = StorageTags.instance();
    private StorageApprovers storageApprovers = StorageApprovers.instance();
    private StorageScratchpad storageScratchpad = StorageScratchpad.instance();


    @Override
    public void init() throws IOException {

        synchronized (Storage.class) {
            storageTransactionInstance.init();
            storageBundleInstance.init();
            storageAddressesInstance.init();
            storageTags.init();
            storageApprovers.init();
            storageScratchpad.init();
            storageTransactionInstance.updateBundleAddressTagApprovers();
            launched = true;
        }
    }

    @Override
    public void shutdown() {

        synchronized (Storage.class) {
            if (launched) {
                storageTransactionInstance.shutdown();
                storageBundleInstance.shutdown();
                storageAddressesInstance.shutdown();
                storageTags.shutdown();
                storageApprovers.shutdown();
                storageScratchpad.shutdown();

                log.info("DB successfully flushed");
            }
        }
    }

    @Override
    public boolean save(Object o) throws Exception {
        return false;
    }

    @Override
    public void delete(Object o) throws Exception {

    }

    @Override
    public boolean update(Object model, String item) throws Exception {
        return false;
    }

    @Override
    public boolean exists(Class<?> model, Hash key) throws Exception {
        return false;
    }

    @Override
    public Object latest(Class<?> model) throws Exception {
        return null;
    }

    @Override
    public Object[] getKeys(Class<?> modelClass) throws Exception {
        return new Object[0];
    }

    @Override
    public boolean get(Object model) throws Exception {
        return false;
    }

    @Override
    public boolean mayExist(Object model) throws Exception {
        return false;
    }

    @Override
    public long count(Class<?> model) throws Exception {
        return 0;
    }

    @Override
    public Hash[] keysStartingWith(Class<?> modelClass, byte[] value) {
        return new Hash[0];
    }

    public static TransactionViewModel loadTransactionViewModel(byte[] mainBuffer) {
        Transaction transaction = new Transaction();
        transaction.type = mainBuffer[TransactionViewModel.TYPE_OFFSET];
        transaction.hash = new Hash(Arrays.copyOfRange(mainBuffer, TransactionViewModel.HASH_OFFSET, TransactionViewModel.HASH_SIZE));
        System.arraycopy(mainBuffer, TransactionViewModel.BYTES_OFFSET, transaction.bytes, 0, TransactionViewModel.BYTES_SIZE);
        transaction.validity = mainBuffer[TransactionViewModel.VALIDITY_OFFSET];
        transaction.arrivalTime = AbstractStorage.value(mainBuffer, TransactionViewModel.ARRIVAL_TIME_OFFSET);
        return new TransactionViewModel(transaction);
    }

    void updateBundleAddressTagAndApprovers(final long transactionPointer) {

        final TransactionViewModel transactionViewModel = loadTransactionViewModel(mainBuffer);
        for (int j = 0; j < numberOfApprovedTransactionsToStore; j++) {
            StorageTransactions.instance().storeTransaction(approvedTransactionsToStore[j], null, false);
        }
        numberOfApprovedTransactionsToStore = 0;

        StorageBundle.instance().updateBundle(transactionPointer, transactionViewModel);
        StorageAddresses.instance().updateAddresses(transactionPointer, transactionViewModel);
        StorageTags.instance().updateTags(transactionPointer, transactionViewModel);
        StorageApprovers.instance().updateApprover(transactionViewModel.getTrunkTransactionHash().bytes(), transactionPointer);

        if (transactionViewModel.getBranchTransactionHash().equals(transactionViewModel.getTrunkTransactionHash())) {
        	StorageApprovers.instance().updateApprover(transactionViewModel.getBranchTransactionHash().bytes(), transactionPointer);
        }
    }
    
    // methods helper
    
    private static Storage instance = new Storage();
    
    private Storage() {}
    
    public static Storage instance() {
		return instance;
	}
}

