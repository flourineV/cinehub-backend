package com.cinehub.showtime.mapper;

import com.cinehub.showtime.client.MovieServiceClient;
import com.cinehub.showtime.dto.response.ShowtimeDetailResponse;
import com.cinehub.showtime.dto.response.ShowtimeResponse;
import com.cinehub.showtime.dto.response.TheaterShowtimesResponse;
import com.cinehub.showtime.entity.Showtime;
import com.cinehub.showtime.repository.SeatRepository;
import com.cinehub.showtime.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShowtimeMapper {

    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final MovieServiceClient movieServiceClient;

    public ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .theaterName(showtime.getTheater().getName())
                .roomId(showtime.getRoom().getId())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .status(showtime.getStatus() != null ? showtime.getStatus().name() : "ACTIVE")
                .build();
    }

    public ShowtimeDetailResponse toShowtimeDetailResponse(Showtime showtime) {
        int totalSeats = seatRepository.countByRoomId(showtime.getRoom().getId());
        long bookedSeats = showtimeSeatRepository.countBookedSeatsByShowtimeId(showtime.getId());
        String movieTitle = movieServiceClient.getMovieTitle(showtime.getMovieId());

        return ShowtimeDetailResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .movieTitle(movieTitle)
                .theaterId(showtime.getTheater().getId())
                .theaterName(showtime.getTheater().getName())
                .provinceId(showtime.getTheater().getProvince().getId())
                .provinceName(showtime.getTheater().getProvince().getName())
                .roomId(showtime.getRoom().getId())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .totalSeats(totalSeats)
                .bookedSeats((int) bookedSeats)
                .availableSeats(totalSeats - (int) bookedSeats)
                .build();
    }

    public TheaterShowtimesResponse.ShowtimeInfo toShowtimeInfo(Showtime showtime) {
        return TheaterShowtimesResponse.ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .roomId(showtime.getRoom().getId().toString())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .build();
    }
}