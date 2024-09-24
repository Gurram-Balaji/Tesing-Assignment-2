import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class BookingTests {

    private String baseUrl = "https://restful-booker.herokuapp.com";

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = baseUrl;
    }

    // Method to read booking data from CSV
    private Object[][] readBookingDataFromFile(String filePath) throws Exception {
        CSVReader csvReader = new CSVReader(new FileReader(filePath));
        String[] nextLine;
        Object[][] bookingsData = new Object[10][7]; // Assuming 10 rows for this example
        int i = 0;
        while ((nextLine = csvReader.readNext()) != null && i < 10) { // Adjust for required rows
            if (i == 0) { // Skip header row
                i++;
                continue;
            }
            bookingsData[i - 1] = nextLine; // Add CSV data to array
            i++;
        }
        csvReader.close();
        return bookingsData;
    }

    // 1. Create, Get, Update, and Delete Multiple Bookings
    @Test(priority = 1)
    public void processMultipleBookings() throws Exception {
        System.out.println("Processing multiple bookings (create, get, update, delete) from CSV file...");

        // Read booking data from CSV
        Object[][] bookingsData = readBookingDataFromFile("src/test/resources/bookings.csv"); // Update the path to your file

        // Iterate over each row in the CSV and process the booking
        for (Object[] bookingData : bookingsData) {
            if (bookingData[0] == null) break; // Stop if the row is empty (EOF)

            // Step 1: Create Booking
            String bookingId = createBooking(bookingData);

            // Step 2: Get and Validate Booking
            getAndValidateBooking(bookingId, bookingData);

            // Step 3: Update and Validate Booking
            updateAndValidateBooking(bookingId);

            // Step 4: Delete and Validate Booking
            deleteAndValidateBooking(bookingId);
        }
    }

    // Create a Booking and return the booking ID
    private String createBooking(Object[] bookingData) {
        System.out.println("Creating a booking...");

        Map<String, Object> bookingDetails = new HashMap<>();
        bookingDetails.put("firstname", bookingData[0]);
        bookingDetails.put("lastname", bookingData[1]);
        bookingDetails.put("totalprice", Integer.parseInt(bookingData[2].toString()));
        bookingDetails.put("depositpaid", Boolean.parseBoolean(bookingData[3].toString()));

        Map<String, String> bookingDates = new HashMap<>();
        bookingDates.put("checkin", bookingData[4].toString());
        bookingDates.put("checkout", bookingData[5].toString());

        bookingDetails.put("bookingdates", bookingDates);
        bookingDetails.put("additionalneeds", bookingData[6].toString());

        // Send POST request
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(bookingDetails)
                .when()
                .post("/booking");

        // Validate response
        Assert.assertEquals(response.getStatusCode(), 200, "Expected status code 200 for successful booking creation.");
        String bookingId = response.jsonPath().getString("bookingid");
        System.out.println("Created Booking ID: " + bookingId);
        return bookingId;
    }

    // Get and validate the booking details
    private void getAndValidateBooking(String bookingId, Object[] bookingData) {
        System.out.println("Fetching and validating the booking with ID: " + bookingId);

        Response response = RestAssured.given()
                .when()
                .get("/booking/" + bookingId);

        Assert.assertEquals(response.getStatusCode(), 200, "Expected status code 200 for successful get.");
        String firstname = response.jsonPath().getString("firstname");
        Assert.assertEquals(firstname, bookingData[0], "First name should match the created booking.");
    }

    // Update and validate the booking
    private void updateAndValidateBooking(String bookingId) {
        System.out.println("Updating and validating the booking with ID: " + bookingId);

        Map<String, Object> updatedBookingDetails = new HashMap<>();
        updatedBookingDetails.put("firstname", "UpdatedFirstName");
        updatedBookingDetails.put("lastname", "UpdatedLastName");
        updatedBookingDetails.put("totalprice", 999);
        updatedBookingDetails.put("depositpaid", false);

        Map<String, String> updatedBookingDates = new HashMap<>();
        updatedBookingDates.put("checkin", "2024-12-01");
        updatedBookingDates.put("checkout", "2024-12-05");

        updatedBookingDetails.put("bookingdates", updatedBookingDates);
        updatedBookingDetails.put("additionalneeds", "Lunch");

        Response response = RestAssured.given().auth().preemptive().basic("admin", "password123")
                .contentType(ContentType.JSON)
                .body(updatedBookingDetails)
                .when()
                .put("/booking/" + bookingId);

        Assert.assertEquals(response.getStatusCode(), 200, "Expected status code 200 for successful update.");
        String updatedFirstname = response.jsonPath().getString("firstname");
        Assert.assertEquals(updatedFirstname, "UpdatedFirstName", "First name should be updated.");
    }

    // Delete and validate the booking
    private void deleteAndValidateBooking(String bookingId) {
        System.out.println("Deleting and validating the booking with ID: " + bookingId);

        Response response = RestAssured.given().auth().preemptive().basic("admin", "password123")
                .when()
                .delete("/booking/" + bookingId);

        Assert.assertEquals(response.getStatusCode(), 201, "Expected status code 201 for successful deletion.");

        // Verify deletion by trying to fetch the booking again
        Response getResponse = RestAssured.given()
                .when()
                .get("/booking/" + bookingId);
        Assert.assertEquals(getResponse.getStatusCode(), 404, "Expected status code 404 after deletion.");
    }
}
