package br.com.poc.redissearches.controller;

import br.com.poc.redissearches.model.Person;
import com.github.javafaker.Faker;
import org.redisson.api.RJsonBucket;
import org.redisson.api.RSearch;
import org.redisson.api.RedissonClient;
import org.redisson.api.search.index.FieldIndex;
import org.redisson.api.search.index.IndexOptions;
import org.redisson.api.search.index.IndexType;
import org.redisson.api.search.query.QueryOptions;
import org.redisson.api.search.query.ReturnAttribute;
import org.redisson.api.search.query.SearchResult;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JacksonCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SearchController {

    private final RedissonClient redisClient;
    private final Faker faker;

    @Autowired
    public SearchController(RedissonClient redisClient, Faker faker) {
        this.redisClient = redisClient;
        this.faker = faker;
    }

    @GetMapping(path = "/names-searches")
    public ResponseEntity searchByName(@RequestParam(value = "name") String name) {
        RSearch s = redisClient.getSearch(StringCodec.INSTANCE);
        createIndexIfNotExists(s);

        SearchResult r = s.search("idxName","@name: " + name + "*", QueryOptions.defaults()
                .returnAttributes(new ReturnAttribute("id"),
                        new ReturnAttribute("name"),
                        new ReturnAttribute("tipoPessoa"))
                .limit(0, 20));

        return new ResponseEntity<>(r.getDocuments(), HttpStatus.OK);
    }

    @PostMapping(path = "/names-searches")
    public ResponseEntity createNamesToSearch(@RequestParam(value = "numberOfNames") Integer numberOfNames) {
        if (numberOfNames != null) {
            try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                    for (int i = 0; i < numberOfNames; i++) {
                        executor.submit(() -> {
                            Person p = new Person(UUID.randomUUID(), faker.name().fullName(), "f");
                            RJsonBucket<Person> b = redisClient.getJsonBucket("name:" + p.name() + p.id().toString(), new JacksonCodec<>(Person.class));
                            b.set(p);
                        });
                    }

                executor.shutdown();
            }
        }

        return new ResponseEntity<>(String.format("Created %2d names.", numberOfNames), HttpStatus.CREATED);
    }

    public void createIndexIfNotExists(RSearch s) {
        // Nao tem uma melhor forma de fazer isso??
        try{
            s.info("idxName");
        } catch (RedisException e) {
                s.createIndex("idxName", IndexOptions.defaults()
                                .on(IndexType.JSON)
                                .prefix("name:"),
                        FieldIndex.text("$..id").as("id"),
                        FieldIndex.text("$..name").as("name"),
                        FieldIndex.text("$..tipoPessoa").as("tipoPessoa"));
            }
        }
    }