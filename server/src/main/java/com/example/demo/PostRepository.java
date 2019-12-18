package com.example.demo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 *
 * @author kkunal
 */

public interface PostRepository extends ReactiveMongoRepository<Post, String> {

}
