package org.possystem.service;

import com.google.gson.Gson;
import org.possystem.dto.PromotionalDiscountResponse;
import org.possystem.dto.SeniorVeteranDiscountResponse;
import org.possystem.dto.CouponValidationResponse;
import org.possystem.entity.TransactionItem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * HTTP Client for communicating with the Discount Engine API.
 * Handles all discount calculation requests using Java 11+ HttpClient.
 *
 * API Base URL: http://localhost:8080/api/discounts (configurable)
 */
public class DiscountApiClient {

    private String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Constructor with default base URL.
     */
    public DiscountApiClient() {
        this("http://localhost:8080/api/discounts");
    }

    /**
     * Constructor with custom base URL.
     * @param baseUrl The base URL for the Discount Engine API
     */
    public DiscountApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
    }

    /**
     * Calculate senior discount (5%).
     * GET /api/discounts/calculate-senior?cartSubtotal=X
     *
     * @param cartSubtotal The current cart subtotal
     * @return SeniorVeteranDiscountResponse with discount details
     * @throws Exception if API call fails
     */
    public SeniorVeteranDiscountResponse calculateSeniorDiscount(double cartSubtotal) throws Exception {
        String url = String.format("%s/calculate-senior?cartSubtotal=%.2f", baseUrl, cartSubtotal);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), SeniorVeteranDiscountResponse.class);
        } else {
            throw new Exception("API Error: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Calculate veteran discount (10%).
     * GET /api/discounts/calculate-veteran?cartSubtotal=X
     *
     * @param cartSubtotal The current cart subtotal
     * @return SeniorVeteranDiscountResponse with discount details
     * @throws Exception if API call fails
     */
    public SeniorVeteranDiscountResponse calculateVeteranDiscount(double cartSubtotal) throws Exception {
        String url = String.format("%s/calculate-veteran?cartSubtotal=%.2f", baseUrl, cartSubtotal);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), SeniorVeteranDiscountResponse.class);
        } else {
            throw new Exception("API Error: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Calculate promotional discount (Buy 2 or More Get 25% Off).
     * GET /api/discounts/calculate-promotion?upc=X&quantity=Y&unitPrice=Z
     *
     * @param upc The product UPC code
     * @param quantity The quantity being purchased
     * @param unitPrice The unit price of the product
     * @return PromotionalDiscountResponse with discount details
     * @throws Exception if API call fails
     */
    public PromotionalDiscountResponse calculatePromotionalDiscount(String upc, int quantity, double unitPrice) throws Exception {
        String url = String.format("%s/calculate-promotion?upc=%s&quantity=%d&unitPrice=%.2f",
                baseUrl, upc, quantity, unitPrice);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), PromotionalDiscountResponse.class);
        } else {
            throw new Exception("API Error: " + response.statusCode() + " - " + response.body());
        }
    }

    /**
     * Check if the Discount Engine API is available.
     * Performs a health check by attempting to reach the API.
     *
     * @return true if API is reachable, false otherwise
     */
    public boolean isApiAvailable() {
        try {
            // Test with a simple senior discount call with $0
            calculateSeniorDiscount(0.0);
            return true;
        } catch (Exception e) {
            System.err.println("Discount API unavailable: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set the base URL for the Discount Engine API.
     * @param baseUrl The new base URL
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Get the current base URL.
     * @return The current base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Validate coupon code.
     * POST /api/discounts/validate-coupon
     *
     * @param couponCode The coupon code to validate
     * @param cartItems The current cart items
     * @param cartSubtotal The current cart subtotal
     * @return CouponValidationResponse with validation result
     * @throws Exception if API call fails
     */
    public CouponValidationResponse validateCoupon(String couponCode, List<TransactionItem> cartItems, double cartSubtotal) throws Exception {
        String url = baseUrl + "/validate-coupon";

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("couponCode", couponCode.toUpperCase()); // Case-insensitive
        requestBody.put("cartSubtotal", cartSubtotal);
        requestBody.put("currentDate", LocalDate.now().toString()); // YYYY-MM-DD format

        // Build cart items array
        List<Map<String, Object>> items = new ArrayList<>();
        for (TransactionItem item : cartItems) {
            if (item.status().equals("ACTIVE")) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("upc", item.upc());
                itemMap.put("quantity", item.quantity());
                itemMap.put("unitPrice", item.unitPrice()); // API expects "unitPrice" not "price"
                items.add(itemMap);
            }
        }
        requestBody.put("cartItems", items);

        String jsonBody = gson.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), CouponValidationResponse.class);
        } else {
            throw new Exception("API Error: " + response.statusCode() + " - " + response.body());
        }
    }
}
