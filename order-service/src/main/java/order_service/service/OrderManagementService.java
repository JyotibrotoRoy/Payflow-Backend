package order_service.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import netscape.javascript.JSObject;
import order_service.dto.OrderRequest;
import order_service.dto.PaymentEvent;
import order_service.entity.Order;
import order_service.entity.OrderStatus;
import order_service.repository.OrderRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.razorpay.Utils.verifyWebhookSignature;

@Service
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public OrderManagementService(
            OrderRepository orderRepository,
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) throws RazorpayException {

        this.orderRepository = orderRepository;
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String createOrder(OrderRequest request) {
        try{
            //1. Payload
            int amountInSubunit = request.getAmount().multiply(new java.math.BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInSubunit);
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            //2. callRazorpay API
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            //3. saving the order to postgreSQL
            Order newOrder = Order.builder()
                    .customerName(request.getCustomerName())
                    .email(request.getEmail())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .paymentMethod(request.getPaymentMethod())
                    .razorpayOrderId(razorpayOrderId)
                    .status(OrderStatus.PENDING)
                    .build();

            orderRepository.save(newOrder);

            //4. wew will return the id to the frontend
            return razorpayOrderId;

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create a RazorPay order: " + e.getMessage());
        }
    }
}
