package com.gurudev.aircnc.domain.trip.service;

import com.gurudev.aircnc.domain.member.entity.Email;
import com.gurudev.aircnc.domain.member.entity.Member;
import com.gurudev.aircnc.domain.member.repository.MemberRepository;
import com.gurudev.aircnc.domain.room.entity.Room;
import com.gurudev.aircnc.domain.room.repository.RoomRepository;
import com.gurudev.aircnc.domain.trip.entity.Trip;
import com.gurudev.aircnc.domain.trip.repository.TripRepository;
import com.gurudev.aircnc.exception.NotFoundException;
import com.gurudev.aircnc.infrastructure.event.TripEvent;
import com.gurudev.aircnc.infrastructure.mail.entity.MailType;
import com.gurudev.aircnc.infrastructure.mail.service.EmailService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TripServiceImpl implements TripService {

  private final TripRepository tripRepository;
  private final MemberRepository memberRepository;
  private final RoomRepository roomRepository;

  private final EmailService tripEmailService;

  @Transactional
  @Override
  public Trip reserve(TripEvent tripEvent) {
    Room room = findRoomById(tripEvent.getRoomId());
    Member guest = findMemberById(tripEvent.getGuestId());

    //TODO: 예약 겹치는지 검증 로직 필요

    tripEmailService.send(Email.toString(guest.getEmail()), room.toMap(), MailType.REGISTER);
    return tripRepository.save(
        new Trip(guest, room,
            tripEvent.getCheckIn(),
            tripEvent.getCheckOut(),
            tripEvent.getTotalPrice(),
            tripEvent.getHeadCount())
    );
  }

  @Override
  public Trip getDetailedById(Long id, Long guestId) {

    return tripRepository.findByIdAndGuestId(id, guestId)
        .orElseThrow(() -> new NotFoundException(Trip.class));
  }

  @Override
  public List<Trip> getByGuestId(Long guestId) {
    return tripRepository.findByGuestId(guestId);
  }

  @Override
  public Trip cancel(Long tripId, Long guestId) {

    Trip trip = findTripByIdAndGuestId(tripId, guestId);

    trip.cancel();

    Member guest = trip.getGuest();
    tripEmailService.send(Email.toString(guest.getEmail()), trip.toMap(), MailType.DELETE);
    return trip;
  }

  private Room findRoomById(Long roomId) {
    return roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException(Room.class));
  }

  private Member findMemberById(Long memberId) {
    return memberRepository.findById(memberId)
        .orElseThrow(() -> new NotFoundException(Member.class));
  }

  private Trip findTripByIdAndGuestId(Long id, Long guestId) {
    return tripRepository.findTripByIdAndGuestId(id, guestId)
        .orElseThrow(() -> new NotFoundException(Trip.class));
  }
}
