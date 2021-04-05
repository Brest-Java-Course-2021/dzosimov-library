package com.epam.brest.webapp;

import com.epam.brest.model.Genre;
import com.epam.brest.model.sample.BookSample;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class BookControllerIT {

    private static final String BOOK_URL = "http://localhost:8060/book";
    private static final String BOOK_ID_URL = "http://localhost:8060/book/1";

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup(){
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void shouldReturnBookPage() throws Exception {
        BookSample bookSample = new BookSample();
        bookSample.setId(1);
        mockMvc.perform(
                MockMvcRequestBuilders.get("/book/new")
                        .sessionAttr("bookSample", bookSample)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(view().name("book"))
                .andExpect(model().attribute("isNew", is(true)));
    }

    @Test
    public void shouldAddBookAndForwardCatalogPageWithGoodMessage() throws Exception {
        BookSample bs = new BookSample("author", "title", Genre.MYSTERY);
        String message = "The book was added";
        server.expect(ExpectedCount.once(), requestTo(BOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("true"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/new")
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }

    @Test
    public void shouldNotAddBookAndForwardCatalogPageWithBadMessage() throws Exception {
        BookSample bs = new BookSample("author", "title", Genre.MYSTERY);
        String message = "The book was not added";
        server.expect(ExpectedCount.once(), requestTo(BOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("false"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/new")
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }

    //TODO:refactor
    @Disabled
    @Test
    public void shouldReturnBookPageWithErrorsWhenBindingResultHasErrors() throws Exception {
        BookSample bs = new BookSample("1", "", Genre.MYSTERY);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/new")
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(model().attributeHasFieldErrors("bookSample", "authors", "title"))
                .andExpect(view().name("book"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetBookPageWithFoundBookById() throws Exception {
        BookSample bs = new BookSample(1,"author", "title", Genre.MYSTERY, 3);
        server.expect(ExpectedCount.once(), requestTo(BOOK_ID_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(bs)));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/book/" + bs.getId())
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(view().name("book"))
                .andExpect(model().attribute("isNew", is(false)))
                .andExpect(model().attribute("bookSample",
                        hasProperty("id", is(bs.getId()))))
                .andExpect(model().attribute("bookSample",
                        hasProperty("authors", is(bs.getAuthors()))))
                .andExpect(model().attribute("bookSample",
                        hasProperty("title", is(bs.getTitle()))))
                .andExpect(model().attribute("bookSample",
                        hasProperty("genre", is(bs.getGenre()))));

    }

    @Test
    public void shouldForwardCatalogPageWhenBookNotFoundById() throws Exception {
        String message = "the book by id 1 not found";
        server.expect(ExpectedCount.once(), requestTo(BOOK_ID_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON));
        mockMvc.perform(
                MockMvcRequestBuilders.get("/book/1")
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(view().name("forward:/catalog"))
                .andExpect(forwardedUrl("/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));

    }

    @Test
    public void shouldForwardToCatalogPageAfterEditBookWithGoodMessage() throws Exception {
        BookSample bs = new BookSample(1,"author", "title", Genre.MYSTERY, 3);
        String message = "The book was edited";
        server.expect(ExpectedCount.once(), requestTo(BOOK_URL))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("true"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/" + bs.getId())
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }

    @Test
    public void shouldForwardToCatalogPageAfterEditBookWithBadMessage() throws Exception {
        BookSample bs = new BookSample(1,"author", "title", Genre.MYSTERY, 3);
        String message = "The book was not edited";
        server.expect(ExpectedCount.once(), requestTo(BOOK_URL))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("false"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/" + bs.getId())
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }

    //TODO:refactor
    @Disabled
    @Test
    public void shouldReturnBookPageAfterEditWithErrorsWhenBindingResultHasErrors() throws Exception {
        BookSample bs = new BookSample(1,"1", "", Genre.MYSTERY, 3);
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/" + bs.getId())
                        .sessionAttr("bookSample", bs)
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(model().attributeHasFieldErrors("bookSample", "authors", "title"))
                .andExpect(view().name("book"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldForwardToCatalogPageAfterDeleteBookWithGoodMessage() throws Exception {
        String message = "The book was removed";
        server.expect(ExpectedCount.once(), requestTo(BOOK_ID_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("true"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/delete/1")
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }

    @Test
    public void shouldForwardToCatalogPageAfterDeleteBookWithBadMessage() throws Exception {
        String message = "The book was not removed";
        server.expect(ExpectedCount.once(), requestTo(BOOK_ID_URL))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("false"));
        mockMvc.perform(
                MockMvcRequestBuilders.post("/book/delete/1")
        ).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.forwardedUrl("/catalog"))
                .andExpect(view().name("forward:/catalog"))
                .andExpect(model().attribute("resultMessage", Matchers.hasToString(message)));
    }
}