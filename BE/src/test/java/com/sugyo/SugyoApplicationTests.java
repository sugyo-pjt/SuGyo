package com.sugyo;

import com.sugyo.domain.game.service.JsonSimilarityComparator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(classes = SugyoApplication.class)
class SugyoApplicationTests {
    @MockitoBean
    private WebClient webClient;


    @Test
    void contextLoads() {
    }

    @Test
    void similarityTest() {

        String file1 = "src/test/resources/test1.json";
        String file2 = "src/test/resources/test2.json";

//        JsonSimilarityComparator.compareJsonFiles(file1, file2);
    }

}
