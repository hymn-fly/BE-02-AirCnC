package com.gurudev.aircnc.controller.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gurudev.aircnc.domain.room.entity.Address;
import com.gurudev.aircnc.domain.trip.entity.Trip;
import com.gurudev.aircnc.domain.trip.service.TripService;
import com.gurudev.aircnc.infrastructure.event.TripEvent;
import com.gurudev.aircnc.infrastructure.mail.service.EmailService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest
public class BaseControllerTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  private TripService tripService;

  protected static String token;

  protected String createJson(Object dto) throws JsonProcessingException {
    return objectMapper.writeValueAsString(dto);
  }

  @MockBean(name = "roomEmailService")
  private EmailService roomEmailService;

  @MockBean(name = "tripEmailService")
  private EmailService tripEmailService;

  @BeforeEach
  void setUp() throws Exception {
    멤버_등록("guest@naver.com", "guest1234!", "게스트", "GUEST");
    멤버_등록("host@naver.com", "host1234!", "호스트", "HOST");
  }

  protected void 멤버_등록(String email, String password, String name, String role) throws Exception {
    ObjectNode memberRegisterRequest = objectMapper.createObjectNode();
    ObjectNode member = memberRegisterRequest.putObject("member");
    member.put("email", email)
        .put("password", password)
        .put("name", name)
        .put("birthDate", "1998-04-21") // random date
        .put("phoneNumber", "010-1234-5678") // random number
        .put("role", role);

    mockMvc.perform(post("/api/v1/members")
            .contentType(MediaType.APPLICATION_JSON)
            .content(memberRegisterRequest.toString()))
        .andExpect(status().isCreated());
  }

  protected Long 로그인(String email, String password) throws Exception {
    ObjectNode loginRequest = objectMapper.createObjectNode();
    ObjectNode member = loginRequest.putObject("member");
    member.put("email", email)
        .put("password", password);

    MvcResult mvcResult = mockMvc.perform(post("/api/v1/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginRequest.toString()))
        .andExpect(status().isOk())
        .andReturn();

    String content = mvcResult.getResponse().getContentAsString();

    token = objectMapper.readValue(content,
        JsonNode.class).get("member").get("token").asText();

    assertThat(token).isNotNull();

    return objectMapper.readValue(content, JsonNode.class).get("member").get("id").asLong();
  }

  protected Long 숙소_등록(String name, Address address, String description,
      String pricePerDay, String capacity) throws Exception {

    InputStream requestInputStream = new FileInputStream(
        "src/test/resources/room-photos-src/photo1.jpeg");
    MockMultipartFile requestImage = new MockMultipartFile("roomPhotosFile", "photo1.jpeg",
        IMAGE_JPEG_VALUE, requestInputStream);

    MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/hosts/rooms")
            .file(requestImage)
            .param("name", name)
            .param("lotAddress", address.getLotAddress())
            .param("roadAddress", address.getRoadAddress())
            .param("detailedAddress", address.getDetailedAddress())
            .param("postCode", address.getPostCode())
            .param("description", description)
            .param("pricePerDay", pricePerDay)
            .param("capacity", capacity)
            .header(AUTHORIZATION, token))
        .andExpect(status().isCreated())
        .andReturn();

    String content = mvcResult.getResponse().getContentAsString();

    return objectMapper.readValue(content, JsonNode.class).get("room").get("id").asLong();
  }

  protected Long 여행_등록_서비스(LocalDate checkIn, LocalDate checkOut,
      int totalPrice, int headCount, Long roomId, Long guestId) {

    Trip reservedTrip = tripService.reserve(
        new TripEvent(guestId, roomId, checkIn, checkOut, headCount, totalPrice));

    return reservedTrip.getId();
  }
}
