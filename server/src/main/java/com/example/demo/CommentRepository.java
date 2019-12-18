package com.example.demo;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 *
 * @author kkunal
 * */
interface CommentRepository extends ReactiveMongoRepository<Comment, String> {

    Flux<Comment> findByPost(PostId id);

}
