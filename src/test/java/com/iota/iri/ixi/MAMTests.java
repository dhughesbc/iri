package com.iota.iri.ixi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.restassured.RestAssured;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.Matchers.containsString;
import static com.jayway.restassured.RestAssured.*;

public class MAMTests {

    private static Gson gson = new GsonBuilder().create();

    static {
        RestAssured.port = 14700;
    }

    /**
     * curl http://localhost:14265 \
     -X POST \
     -H 'Content-Type: application/json' \
     -d '{"command": "MAM.generateMerkleKeys", "seed": "MYSEEDSARETHEBEST9SEEDSWHODONTUSEMYSEEDARESADALLSEEDSSHOULDBEZEROLENGTHORGREATER9", "start": 0, "count": 13, "size": 81}'
     */
    @Test
    public void shouldTestGenerateMerkleTree() {

        final Map<String, Object> request = new HashMap<>();
        request.put("command", "MAM.generateMerkleKeys");
        request.put("seed", "MYSEEDSARETHEBEST9SEEDSWHODONTUSEMYSEEDARESADALLSEEDSSHOULDBEZEROLENGTHORGREATER9");
        request.put("start", 0);
        request.put("count", 13);
        request.put("size", 81);
        given().
                contentType("application/json").
                body(gson.toJson(request)).
                when().
                post("/").
                then().
                body(containsString("merkleTree")).
                body(containsString("duration")).
                statusCode(200);
    }
}
