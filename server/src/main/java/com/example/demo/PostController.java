package com.example.demo;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author kkunal
 */

@RestController()
@RequestMapping(value = "/posts")
class PostController {

    @Autowired
    private Client client;

    private PostRepository posts;
    private CommentRepository comments;

    public PostController(PostRepository posts, CommentRepository comments) {
        this.posts = posts;
        this.comments = comments;
    }

    @GetMapping("")
    public Flux<Post> all(@RequestParam(value = "q", required = false) String q,
                          @RequestParam(value = "page", defaultValue = "0") long page,
                          @RequestParam(value = "size", defaultValue = "10") long size) {
        return filterPublishedPostsByKeyword(q)
                .sort(comparing(Post::getCreatedDate).reversed())
                .skip(page * size).take(size);
    }

    @GetMapping(value = "/count")
    public Mono<Count> count(@RequestParam(value = "q", required = false) String q) {
        return filterPublishedPostsByKeyword(q).count().log().map(Count::new);
    }

    private Flux<Post> filterPublishedPostsByKeyword(String q) {
        return this.posts.findAll()
                .filter(p -> Post.Status.PUBLISHED == p.getStatus())
                .filter(
                        p -> Optional.ofNullable(q)
                                .map(key -> p.getTitle().contains(key) || p.getContent().contains(key))
                                .orElse(true)
                );
    }

    @PostMapping("")
    public Mono<Post> create(@RequestBody @Valid Post post) {
        return this.posts.save(post);
    }

    @GetMapping("/{id}")
    public Mono<Post> get(@PathVariable("id") String id) {
        return this.posts.findById(id).switchIfEmpty(Mono.error(new PostNotFoundException(id)));
    }

    @PutMapping("/{id}")
    public Mono<Post> update(@PathVariable("id") String id, @RequestBody @Valid Post post) {
        return this.posts.findById(id)
                .switchIfEmpty(Mono.error(new PostNotFoundException(id)))
                .map(p -> {
                    p.setTitle(post.getTitle());
                    p.setContent(post.getContent());

                    return p;
                })
                .flatMap(this.posts::save);
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(NO_CONTENT)
    public Mono<Void> updateStatus(@PathVariable("id") String id, @RequestBody @Valid StatusUpdateRequest status) {
        return this.posts.findById(id)
                .switchIfEmpty(Mono.error(new PostNotFoundException(id)))
                .map(p -> {
                    // TODO: check if the current user is author or it has ADMIN role.
                    p.setStatus(Post.Status.valueOf(status.getStatus()));

                    return p;
                })
                .flatMap(this.posts::save)
                .flatMap((p) -> Mono.empty());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public Mono<Void> delete(@PathVariable("id") String id) {
        return this.posts.findById(id)
                .switchIfEmpty(Mono.error(new PostNotFoundException(id)))
                .flatMap(this.posts::delete);
    }

    @GetMapping("/{id}/comments")
    public Flux<Comment> getCommentsOf(@PathVariable("id") String id) {
        return this.comments.findByPost(new PostId(id));
    }

    @GetMapping("/{id}/comments/count")
    public Mono<Count> getCommentsCountOf(@PathVariable("id") String id) {
        return this.comments.findByPost(new PostId()).count().log().map(Count::new);
    }

    @PostMapping("/{id}/comments")
    public Mono<Comment> createCommentsOf(@PathVariable("id") String id, @RequestBody @Valid CommentForm form) {
        Comment comment = Comment.builder()
                .post(new PostId())
                .content(form.getContent())
                .build();

        return this.comments.save(comment);
    }

    @PostMapping("/create")
    public String create(@RequestBody UserElasticSearch user) throws IOException {

        IndexResponse response = client.prepareIndex("users", "kunal", user.getUserId())
                .setSource(jsonBuilder()
                        .startObject()
                        .field("name", user.getName())
                        .field("userSettings", user.getUserSettings())
                        .endObject()
                )
                .get();
        System.out.println("response id:"+response.getId());
        return response.getResult().toString();
    }


    @GetMapping("/view/{id}")
    public Map<String, Object> view(@PathVariable final String id) {
        GetResponse getResponse = client.prepareGet("users", "kunal", id).get();
        System.out.println(getResponse.getSource());


        return getResponse.getSource();
    }
    @GetMapping("/view/name/{field}")
    public Map<String, Object> searchByName(@PathVariable final String field) {
        Map<String,Object> map = null;
        SearchResponse response = client.prepareSearch("users")
                .setTypes("kunal")
                .setSearchType(SearchType.QUERY_AND_FETCH)
                .setQuery(QueryBuilders.matchQuery("name", field))
                .get()
                ;
        List<SearchHit> searchHits = Arrays.asList(response.getHits().getHits());
        map =   searchHits.get(0).getSource();
        return map;

    }

    @GetMapping("/update/{id}")
    public String update(@PathVariable final String id) throws IOException {

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("users")
                .type("kunal")
                .id(id)
                .doc(jsonBuilder()
                        .startObject()
                        .field("name", "Kumar Kunal")
                        .endObject());
        try {
            UpdateResponse updateResponse = client.update(updateRequest).get();
            System.out.println(updateResponse.status());
            return updateResponse.status().toString();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e);
        }
        return "Exception";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable final String id) {

        DeleteResponse deleteResponse = client.prepareDelete("users", "kunal", id).get();

        System.out.println(deleteResponse.getResult().toString());
        return deleteResponse.getResult().toString();
    }

}
