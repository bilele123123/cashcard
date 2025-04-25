package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.coyote.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnCashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);

        String name = documentContext.read("$.owner");
		assertThat(name).isEqualTo("Thai");
	}

	@Test
	void shouldNotReturnCashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/1000", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateNewCashCard() {
		CashCard test = new CashCard(null, 250.00, "Thai");
		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("Thai", "abc123")
				.postForEntity("/cashcards", test, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfTest = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity(locationOfTest, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		String owner = documentContext.read("$.owner");
		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
		assertThat(owner).isEqualTo("Thai");
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(response.getBody());

		int cashCardCount = dc.read("$.length()");
		assertThat(cashCardCount).isEqualTo(4);

		JSONArray ids = dc.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 102, 100, 101);

		JSONArray amounts = dc.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 111.45, 1.00, 150.00);

		JSONArray owners = dc.read("$..owner");
		assertThat(owners).containsExactlyInAnyOrder("Thai", "Thai", "Thai", "Thai");

		response = restTemplate
				.withBasicAuth("Mike", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		dc = JsonPath.parse(response.getBody());
		cashCardCount = dc.read("$.length()");
		ids = dc.read("$..id");
		amounts = dc.read("$..amount");
		owners = dc.read("$..owner");

		assertThat(cashCardCount).isEqualTo(1);
		assertThat(amounts).containsExactlyInAnyOrder(11.58);
		assertThat(ids).containsExactlyInAnyOrder(103);
		assertThat(owners).containsExactlyInAnyOrder("Mike");
	}

	@Test
	void shouldReturnPageOfCashCards() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards?page=0&size=1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(response.getBody());
		JSONArray page = dc.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnAscendingSortedPageOfCashCard() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,asc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(response.getBody());
		JSONArray read = dc.read("$[*]");

		double amount = dc.read("$[0].amount");
		assertThat(amount).isEqualTo(1.00);
	}

	@Test
	void shouldReturnDescendingSortedPageOfCashCard() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(response.getBody());
		JSONArray read = dc.read("$[*]");

		double amount = dc.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnSortedPageOfCashCardWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(response.getBody());
		JSONArray page = dc.read("$[*]");
		assertThat(page.size()).isEqualTo(4);

		JSONArray amounts = dc.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 111.45, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnCashCardWithBadCredentials() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("wrong-user", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("Thai", "wrong-password")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejectWhenNotCorrectRole() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("user-no-cards", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void rejectWhenNotCardOwner() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/103", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext dc = JsonPath.parse(getResponse.getBody());

		Number id = dc.read("$.id");
		Double amount = dc.read("$.amount");

		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateCardThatDoesNotExist() {
		CashCard updateCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(updateCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateCardThatIsOwnedBySomeoneElse() {
		CashCard updateCard = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> request = new HttpEntity<>(updateCard);
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.exchange("/cashcards/103", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCard() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteCardThatDoesNotExist() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Thai", "abc123")
				.exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteCardThatIsOwnedBySomeoneElse() {
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("Mike", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("Thai", "abc123")
				.getForEntity("/cashcards/99", String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
