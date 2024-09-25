import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class HotelManagementSystem {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/hotel";
    private static final String USER = "root";
    private static final String PASSWORD = "250225";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Class.forName("org.h2.Driver");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    // Method to retrieve reservation details by ID from the database
    private Reservation getReservationById(int reservationId) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT guest_name, check_in_date, check_out_date FROM reservations WHERE id=?")) {

            preparedStatement.setInt(1, reservationId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String guestName = resultSet.getString("guest_name");
                    Date checkInDate = resultSet.getDate("check_in_date");
                    Date checkOutDate = resultSet.getDate("check_out_date");

                    return new Reservation(reservationId, guestName, checkInDate, checkOutDate);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // Return null if the reservation is not found
    }
    public static void main(String[] args) throws IOException {
        createTable();

        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/hotel", new HotelHandler());
        server.createContext("/update", new HotelHandler());
        server.createContext("/delete", new HotelHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server is running on port 8080. Open your browser and visit http://localhost:8080/hotel");
        while (true) {
            System.out.println("\n1. Create Reservation");
            System.out.println("2. Display Reservations");
            System.out.println("3. Exit");

            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    createReservation();
                    break;
                case 2:
                    displayReservations();
                    break;
                case 3:
                    server.stop(0);
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void createTable() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS reservations (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "guest_name VARCHAR(255) NOT NULL, " +
                    "check_in_date DATE NOT NULL, " +
                    "check_out_date DATE NOT NULL)"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createReservation() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO reservations (guest_name, check_in_date, check_out_date) VALUES (?, ?, ?)")) {
    
            Scanner scanner = new Scanner(System.in);
    
            System.out.print("Enter guest name: ");
            String guestName = scanner.nextLine();
    
            System.out.print("Enter check-in date (YYYY-MM-DD): ");
            String checkInDateStr = scanner.nextLine();
            try {
                Date checkInDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkInDateStr);
                preparedStatement.setString(1, guestName);
                preparedStatement.setDate(2, new java.sql.Date(checkInDate.getTime()));
    
                System.out.print("Enter check-out date (YYYY-MM-DD): ");
                String checkOutDateStr = scanner.nextLine();
                Date checkOutDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkOutDateStr);
                preparedStatement.setDate(3, new java.sql.Date(checkOutDate.getTime()));
    
                preparedStatement.executeUpdate(); // Execute the update statement to insert the reservation
                System.out.println("Reservation created successfully!");
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please enter the date in the format YYYY-MM-DD.");
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void displayReservations() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT id, guest_name, check_in_date, check_out_date FROM reservations")) {
    
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int reservationId = resultSet.getInt("id");
                    String guestName = resultSet.getString("guest_name");
                    Date checkInDate = resultSet.getDate("check_in_date");
                    Date checkOutDate = resultSet.getDate("check_out_date");
    
                    System.out.println("Reservation ID: " + reservationId +
                            ", Guest: " + guestName +
                            ", Check-in: " + (checkInDate != null ? checkInDate.toString() : "N/A") +
                            ", Check-out: " + (checkOutDate != null ? checkOutDate.toString() : "N/A"));
                }
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


// static class HotelHandler implements HttpHandler {
//     @Override
//     public void handle(HttpExchange exchange) throws IOException {
//         String method = exchange.getRequestMethod();
//         String path = exchange.getRequestURI().getPath();

//         if (method.equals("GET")) {
//             if (path.equals("/hotel")) {
//                 // Handle GET request (display the form)
//                 displayReservationsPage(exchange);
//             } else if (path.equals("/update")) {
//                 // Handle GET request to /update (display update form)
//                 displayUpdateForm(exchange);
//             }
//         } else if (method.equals("POST")) {
//             // Handle POST request (process form submission)
//             processReservationForm(exchange);
//         }else if (method.equals("GET") && path.equals("/delete")) {
//             // Handle GET request to /delete (perform deletion)
//             processDeleteRequest(exchange);
//         }
//     }
//     }
static class HotelHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("GET")) {
            if (path.equals("/hotel")) {
                // Handle GET request (display the form)
                displayReservationsPage(exchange);
            } else if (path.equals("/update")) {
                // Handle GET request to /update (display update form)
                displayUpdateForm(exchange);
            } else if (path.equals("/delete")) {
                // Handle GET request to /delete (perform deletion)
                processDeleteRequest(exchange);
            }
        } else if (method.equals("POST")) {
            // Handle POST request (process form submission)
            processReservationForm(exchange);
        }
    }
     private void serveHTMLPage(HttpExchange exchange, String fileName) throws IOException {
        String filePath =   fileName;
        File file = new File(filePath);
        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}


    private static void displayReservationsPage(HttpExchange exchange) throws IOException {
        StringBuilder response = new StringBuilder("<html><body>");
        response.append("<h1>Reservations</h1>");
    
        // Retrieve reservations from the database
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT id, guest_name, check_in_date, check_out_date FROM reservations");
             ResultSet resultSet = preparedStatement.executeQuery()) {
    
            response.append("<table border='1'><tr><th>ID</th><th>Guest Name</th><th>Check-in Date</th><th>Check-out Date</th><th>Action</th></tr>");
            while (resultSet.next()) {
                int reservationId = resultSet.getInt("id");
                String guestName = resultSet.getString("guest_name");
                Date checkInDate = resultSet.getDate("check_in_date");
                Date checkOutDate = resultSet.getDate("check_out_date");
    
                response.append("<tr>")
                        .append("<td>").append(reservationId).append("</td>")
                        .append("<td>").append(guestName).append("</td>")
                        .append("<td>").append(checkInDate != null ? checkInDate.toString() : "N/A").append("</td>")
                        .append("<td>").append(checkOutDate != null ? checkOutDate.toString() : "N/A").append("</td>")
                        .append("<td>")
                        .append("<a href='/update?id=").append(reservationId).append("'>Update</a> | ")
                        // .append("<a href='/delete?id=").append(reservationId).append("'>Delete</a>")
                        .append("<a href='/delete?id=").append(reservationId).append("&action=delete'>Delete</a>")

                        .append("</td>")
                        .append("</tr>");
            }
            response.append("</table>");
    
        } catch (SQLException e) {
            e.printStackTrace();
            response.append("<p>Error retrieving reservations. Please try again.</p>");
        }
    
        // Display the reservation creation form
        response.append("<br><h2>Create Reservation</h2>");
        response.append("<form action='/hotel' method='post'>");
        response.append("Guest Name: <input type='text' name='guestName' required><br>");
        response.append("Check-in Date (YYYY-MM-DD): <input type='text' name='checkInDate' required><br>");
        response.append("Check-out Date (YYYY-MM-DD): <input type='text' name='checkOutDate' required><br>");
        response.append("<input type='hidden' name='action' value='create'>");
        response.append("<input type='submit' value='Create Reservation'>");
        response.append("</form>");
    
        response.append("</body></html>");
    
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
    
    // private static void processReservationForm(HttpExchange exchange) throws IOException {
    //     // Extract form data from the request
    //     String requestBody = new String(exchange.getRequestBody().readAllBytes());
    //     String[] formData = requestBody.split("&");
    
    //     String action = formData[3].split("=")[1]; // Extract the action parameter
    
    //     if (action.equals("create")) {
    //         // Process form data and create reservation
    //         createReservation(formData, exchange);
    //     } else if (action.equals("update")) {
            
    //         // Process form data and update reservation
    //         updateReservation(formData, exchange);
    //     } else if (action.equals("delete")) {
    //         System.out.println("Hello, delete in elseif!");
    //         // Process form data and delete reservation
    //         deleteReservation(formData, exchange);
    //     }
    // }
    // private static void processReservationForm(HttpExchange exchange) throws IOException {
    //     // Extract form data from the request
    //     String requestBody = new String(exchange.getRequestBody().readAllBytes());
    //     String[] formData = requestBody.split("&");
    
    //     // Extract the action parameter
    //     String action = "";
    //     for (String data : formData) {
    //         String[] pair = data.split("=");
    //         String key = pair[0];
    //         String value = pair.length > 1 ? pair[1] : "";
    //         if (key.equals("action")) {
    //             action = value;
    //             break;
    //         }
    //     }
    
    //     // Based on the action, call the appropriate method
    //     if (action.equals("create")) {
    //         createReservation(formData, exchange);
    //     } else if (action.equals("update")) {
    //         updateReservation(formData, exchange);
    //     } else if (action.equals("delete")) {
    //         deleteReservation(formData, exchange);
    //     }
    // }
    // private static void processReservationForm(HttpExchange exchange) throws IOException {
    //     // Extract form data from the request
    //     String requestBody = new String(exchange.getRequestBody().readAllBytes());
    //     String[] formData = requestBody.split("&");
    
    //     // Extract the action parameter
    //     String action = "";
    //     for (String data : formData) {
    //         String[] pair = data.split("=");
    //         String key = pair[0];
    //         String value = pair.length > 1 ? pair[1] : "";
    //         if (key.equals("action")) {
    //             action = value;
    //             break;
    //         }
    //     }
    
    //     // Debugging: print out the action parameter
    //     System.out.println("Action parameter: " + action);
    
    //     // Based on the action, call the appropriate method
    //     if (action.equals("create")) {
    //         createReservation(formData, exchange);
    //     } else if (action.equals("update")) {
    //         updateReservation(formData, exchange);
    //     } else if (action.equals("delete")) {
    //         // deleteReservation(formData, exchange);
    //         deleteReservation(reservationId, exchange);

    //     }
    // }
    private static void processReservationForm(HttpExchange exchange) throws IOException {
        // Extract form data from the request
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        String[] formData = requestBody.split("&");
    
        // Extract the action parameter
        String action = "";
        int reservationId = -1; // Default value
        for (String data : formData) {
            String[] pair = data.split("=");
            String key = pair[0];
            String value = pair.length > 1 ? pair[1] : "";
            if (key.equals("action")) {
                action = value;
            } else if (key.equals("reservationId")) {
                reservationId = Integer.parseInt(value);
            }
        }
    
        // Debugging: print out the action parameter
        System.out.println("Action parameter: " + action);
    
        // Based on the action, call the appropriate method
        if (action.equals("create")) {
            createReservation(formData, exchange);
        } else if (action.equals("update")) {
            updateReservation(formData, exchange);
        } else if (action.equals("delete")) {
            deleteReservation(reservationId, exchange);
        }
    }
    
    
    
    private static void createReservation(String[] formData, HttpExchange exchange) throws IOException {
        String guestName = formData[0].split("=")[1];
        String checkInDateStr = formData[1].split("=")[1];
        String checkOutDateStr = formData[2].split("=")[1];
    
        // Process form data and create reservation
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO reservations (guest_name, check_in_date, check_out_date) VALUES (?, ?, ?)")) {
    
            Date checkInDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkInDateStr);
            preparedStatement.setString(1, guestName);
            preparedStatement.setDate(2, new java.sql.Date(checkInDate.getTime()));
    
            Date checkOutDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkOutDateStr);
            preparedStatement.setDate(3, new java.sql.Date(checkOutDate.getTime()));
    
            preparedStatement.executeUpdate();
    
            // Redirect to display reservations after creating a reservation
            String redirectUrl = "/hotel";
            exchange.getResponseHeaders().set("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
        } catch (ParseException | SQLException e) {
            e.printStackTrace();
            String errorMessage = "<html><body><h1>Error creating reservation. Please try again.</h1></body></html>";
            exchange.sendResponseHeaders(500, errorMessage.length());
            OutputStream os = exchange.getResponseBody();
            os.write(errorMessage.getBytes());
            os.close();
        }
    }
    

    private static void displayUpdateForm(HttpExchange exchange) throws IOException {
        // Extract reservation ID from the query parameters
        String query = exchange.getRequestURI().getQuery();
        String[] params = query.split("&");
        int reservationId = Integer.parseInt(params[0].split("=")[1]);
    
        // Retrieve reservation details based on ID from the database
        HotelManagementSystem hotelSystem = new HotelManagementSystem();
        Reservation reservation = hotelSystem.getReservationById(reservationId);
    
        // Display the update form with pre-filled data
        StringBuilder response = new StringBuilder("<html><body>");
        response.append("<h1>Update Reservation</h1>");
        
        if (reservation != null) {
            // Add the form fields for updating reservations with pre-filled data
            response.append("<form action='/hotel' method='post'>");
            response.append("Guest Name: <input type='text' name='guestName' value='" + reservation.getGuestName() + "' required><br>");
            response.append("Check-in Date (YYYY-MM-DD): <input type='text' name='checkInDate' value='" + reservation.getCheckInDate() + "' required><br>");
            response.append("Check-out Date (YYYY-MM-DD): <input type='text' name='checkOutDate' value='" + reservation.getCheckOutDate() + "' required><br>");
            response.append("<input type='hidden' name='action' value='update'>");
            response.append("<input type='hidden' name='reservationId' value='" + reservationId + "'>");
            response.append("<input type='submit' value='Update Reservation'>");
            response.append("</form>");
        } else {
            // Handle the case where the reservation is not found
            response.append("<p>Reservation not found.</p>");
        }
    
        response.append("<br><a href='/hotel'>Back to Reservations</a>");
        response.append("</body></html>");
    
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
    
    

    private static void updateReservation(String[] formData, HttpExchange exchange) throws IOException {

        int reservationId = 0;
        String guestName = "";
        String checkInDateStr = "";
        String checkOutDateStr = "";
        
        // Loop through the formData array to find the parameters
        for (String data : formData) {
            String[] pair = data.split("=");
            String key = pair[0];
            String value = pair.length > 1 ? pair[1] : "";
            
            if (key.equals("reservationId")) {
                reservationId = Integer.parseInt(value);
            } else if (key.equals("guestName")) {
                guestName = value;
            } else if (key.equals("checkInDate")) {
                checkInDateStr = value;
            } else if (key.equals("checkOutDate")) {
                checkOutDateStr = value;
            }
        }
        
        // Process form data and update reservation
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE reservations SET guest_name=?, check_in_date=?, check_out_date=? WHERE id=?")) {
    
            Date checkInDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkInDateStr);
            preparedStatement.setString(1, guestName);
            preparedStatement.setDate(2, new java.sql.Date(checkInDate.getTime()));
    
            Date checkOutDate = new SimpleDateFormat("yyyy-MM-dd").parse(checkOutDateStr);
            preparedStatement.setDate(3, new java.sql.Date(checkOutDate.getTime()));
    
            preparedStatement.setInt(4, reservationId);
    
            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println(rowsAffected + " row(s) updated."); // Log the number of rows updated
    
            // Redirect to display reservations after updating a reservation
            String redirectUrl = "/hotel";
            exchange.getResponseHeaders().set("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
            // exchange.close();
    
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Hello, update in catch!");
            String errorMessage = "<html><body><h1>Error updating reservation. Please try again.</h1></body></html>";
            exchange.sendResponseHeaders(500, errorMessage.length());
            // exchange.close();
            OutputStream os = exchange.getResponseBody();
            os.write(errorMessage.getBytes());
            os.close();
        }
    }
    private static void processDeleteRequest(HttpExchange exchange) throws IOException {
        // Extract reservation ID from the query parameters
        String query = exchange.getRequestURI().getQuery();
        String[] params = query.split("&");
        int reservationId = Integer.parseInt(params[0].split("=")[1]);
    
        // Process delete request
        deleteReservation(reservationId, exchange);
    }
    private static void deleteReservation(int reservationId, HttpExchange exchange) throws IOException {
        // Process form data and delete reservation
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "DELETE FROM reservations WHERE id=?")) {
            
            preparedStatement.setInt(1, reservationId);
            int affectedRows = preparedStatement.executeUpdate();
    
            if (affectedRows > 0) {
                System.out.println("Reservation with ID " + reservationId + " deleted successfully.");
            } else {
                System.out.println("No reservation found with ID " + reservationId);
            }
    
            // Redirect to display reservations after deleting a reservation
            String redirectUrl = "/hotel";
            exchange.getResponseHeaders().set("Location", redirectUrl);
            exchange.sendResponseHeaders(302, -1);
        } catch (SQLException e) {
            System.out.println("Error deleting reservation: " + e.getMessage());
            e.printStackTrace();
    
            String errorMessage = "<html><body><h1>Error deleting reservation. Please try again.</h1></body></html>";
            exchange.sendResponseHeaders(500, errorMessage.length());
            OutputStream os = exchange.getResponseBody();
            os.write(errorMessage.getBytes());
            os.close();
        }
    }
    
    
    
    // private static void deleteReservation(String[] formData, HttpExchange exchange) throws IOException {
    //     System.out.println("Hello, delete entered!");

    //     // Extract reservation ID from the form data
    //     int reservationId = Integer.parseInt(formData[0].split("=")[1]);
    
    //     // Process form data and delete reservation
    //     try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    //          PreparedStatement preparedStatement = connection.prepareStatement(
    //                  "DELETE FROM reservations WHERE id=?")) {
            
    //         preparedStatement.setInt(1, reservationId);
    //         int affectedRows = preparedStatement.executeUpdate();
    
    //         if (affectedRows > 0) {
    //             System.out.println("Reservation with ID " + reservationId + " deleted successfully.");
    //         } else {
    //             System.out.println("No reservation found with ID " + reservationId);
    //         }
    
    //         // Redirect to display reservations after deleting a reservation
    //         String redirectUrl = "/hotel";
    //         exchange.getResponseHeaders().set("Location", redirectUrl);
    //         exchange.sendResponseHeaders(302, -1);
    //     } catch (SQLException e) {
    //         System.out.println("Error deleting reservation: " + e.getMessage());
    //         e.printStackTrace();
    
    //         String errorMessage = "<html><body><h1>Error deleting reservation. Please try again.</h1></body></html>";
    //         exchange.sendResponseHeaders(500, errorMessage.length());
    //         OutputStream os = exchange.getResponseBody();
    //         os.write(errorMessage.getBytes());
    //         os.close();
    //     }
    // }
    
  

    // private static void deleteReservation(String[] formData, HttpExchange exchange) throws IOException {
    //     System.out.println("Hello, function in delete!");
    //     // Extract reservation ID from the URL query parameter
    //     int reservationId = Integer.parseInt(formData[3].split("=")[1]);
    //     int affectedrows = 0;

    //     // Process form data and delete reservation
    //     try (Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    //          PreparedStatement preparedStatement = connection.prepareStatement(
    //                  "DELETE FROM reservations WHERE id=?")) {
    //                     System.out.println("delete");

    //         preparedStatement.setInt(1, reservationId);
    //         // preparedStatement.addBatch();
    //         // preparedStatement.executeBatch();
    //         affectedrows = preparedStatement.executeUpdate();

    //         // preparedStatement.executeUpdate();
    //         // preparedStatement.close();
    //         // Redirect to display reservations after deleting a reservation
    //         String redirectUrl = "/hotel";
    //         exchange.getResponseHeaders().set("Location", redirectUrl);
    //         exchange.sendResponseHeaders(302, -1);
    //         // exchange.close();
    //     } catch (SQLException e) {
    //         System.out.println(e.getMessage());
    //         e.printStackTrace();
    //         String errorMessage = "<html><body><h1>Error deleting reservation. Please try again.</h1></body></html>";
    //         exchange.sendResponseHeaders(500, errorMessage.length());
    //         OutputStream os = exchange.getResponseBody();
    //         os.write(errorMessage.getBytes());
    //         os.close();
    //     }
    // }
}

