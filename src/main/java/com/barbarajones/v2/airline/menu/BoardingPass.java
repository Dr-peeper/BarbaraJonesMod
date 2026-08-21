package com.barbarajones.v2.airline.menu;

public class BoardingPass {
    public String flightId;
    public String seatNumber;
    public String departureCity;
    public String arrivalCity;
    public long departureTime;
    public boolean isValid;

    public BoardingPass(String flightId, String seatNumber, String departureCity,
                        String arrivalCity, long departureTime) {
        this.flightId = flightId;
        this.seatNumber = seatNumber;
        this.departureCity = departureCity;
        this.arrivalCity = arrivalCity;
        this.departureTime = departureTime;
        this.isValid = true;
    }

    @Override
    public String toString() {
        return String.format("Flight %s | Seat %s | %s → %s",
            flightId, seatNumber, departureCity, arrivalCity);
    }

    public String getSeatRow() {
        if (seatNumber.isEmpty()) return "";
        return seatNumber.substring(0, seatNumber.length() - 1);
    }

    public String getSeatColumn() {
        if (seatNumber.isEmpty()) return "";
        return seatNumber.substring(seatNumber.length() - 1);
    }
}
