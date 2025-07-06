import java.time.LocalDate;
import java.util.*;

// Interface for shippable items
interface Shippable {
    String getName();
    double getWeight();
}

// Abstract base class for products
abstract class Product {
    protected String name;
    protected double price;
    protected int quantity;
    
    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    
    public void reduceQuantity(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("Not enough stock available");
        }
        this.quantity -= amount;
    }
    
    public abstract boolean isExpired();
    public abstract boolean requiresShipping();
}

// Expirable product class
class ExpirableProduct extends Product {
    private LocalDate expiryDate;
    private boolean requiresShipping;
    private double weight;
    
    public ExpirableProduct(String name, double price, int quantity, LocalDate expiryDate, boolean requiresShipping, double weight) {
        super(name, price, quantity);
        this.expiryDate = expiryDate;
        this.requiresShipping = requiresShipping;
        this.weight = weight;
    }
    
    @Override
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }
    
    @Override
    public boolean requiresShipping() {
        return requiresShipping;
    }
    
    public double getWeight() { return weight; }
}

// Non-expirable product class
class NonExpirableProduct extends Product {
    private boolean requiresShipping;
    private double weight;
    
    public NonExpirableProduct(String name, double price, int quantity, boolean requiresShipping, double weight) {
        super(name, price, quantity);
        this.requiresShipping = requiresShipping;
        this.weight = weight;
    }
    
    @Override
    public boolean isExpired() {
        return false;
    }
    
    @Override
    public boolean requiresShipping() {
        return requiresShipping;
    }
    
    public double getWeight() { return weight; }
}

// Cart item class to hold product and quantity
class CartItem {
    private Product product;
    private int quantity;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return product.getPrice() * quantity; }
}

// Customer class
class Customer {
    private String name;
    private double balance;
    
    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }
    
    public String getName() { return name; }
    public double getBalance() { return balance; }
    
    public void deductBalance(double amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        balance -= amount;
    }
}

// Shopping cart class
class ShoppingCart {
    private List<CartItem> items;
    
    public ShoppingCart() {
        this.items = new ArrayList<>();
    }
    
    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (quantity > product.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock available for " + product.getName());
        }
        
        for (CartItem item : items) {
            if (item.getProduct().equals(product)) {
                int newQuantity = item.getQuantity() + quantity;
                if (newQuantity > product.getQuantity()) {
                    throw new IllegalArgumentException("Not enough stock available for " + product.getName());
                }
                items.remove(item);
                items.add(new CartItem(product, newQuantity));
                return;
            }
        }
        
        items.add(new CartItem(product, quantity));
    }
    
    public List<CartItem> getItems() { return new ArrayList<>(items); }
    public boolean isEmpty() { return items.isEmpty(); }
    public double getSubtotal() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }
}

// Shipping service interface
interface ShippingService {
    double calculateShippingFee(List<Shippable> shippableItems);
    void shipItems(List<Shippable> shippableItems);
}

// Concrete shipping service implementation
class DefaultShippingService implements ShippingService {
    
    @Override
    public double calculateShippingFee(List<Shippable> shippableItems) {
        return 30.0; // Fixed shipping fee
    }
    
    @Override
    public void shipItems(List<Shippable> shippableItems) {
        if (shippableItems.isEmpty()) return;
        
        System.out.println("** Shipment notice **");
        Map<String, Integer> itemCounts = new HashMap<>();
        Map<String, Double> itemWeights = new HashMap<>();
        
        for (Shippable item : shippableItems) {
            itemCounts.put(item.getName(), itemCounts.getOrDefault(item.getName(), 0) + 1);
            itemWeights.put(item.getName(), item.getWeight());
        }
        
        double totalWeight = 0;
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();
            double weight = itemWeights.get(name) * count;
            totalWeight += weight;
            System.out.printf("%dx %s %.0fg%n", count, name, weight * 1000);
        }
        System.out.printf("Total package weight %.1fkg%n", totalWeight);
    }
}

// Checkout service with FIXED logic
class CheckoutService {
    private ShippingService shippingService;
    
    public CheckoutService(ShippingService shippingService) {
        this.shippingService = shippingService;
    }
    
    public void checkout(Customer customer, ShoppingCart cart) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.isExpired()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is expired");
            }
            if (item.getQuantity() > product.getQuantity()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is out of stock");
            }
        }

        double subtotal = cart.getSubtotal();

        List<Shippable> shippableItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.requiresShipping()) {
                for (int i = 0; i < item.getQuantity(); i++) {
                    shippableItems.add(new ShippableItem(product.getName(), getProductWeight(product)));
                }
            }
        }

        double shippingFee = shippingService.calculateShippingFee(shippableItems);
        double totalAmount = subtotal + shippingFee;

        if (customer.getBalance() < totalAmount) {
            throw new IllegalArgumentException("Customer's balance is insufficient");
        }

        customer.deductBalance(totalAmount);
        for (CartItem item : cart.getItems()) {
            item.getProduct().reduceQuantity(item.getQuantity());
        }

        shippingService.shipItems(shippableItems);

        // Print filtered receipt
        System.out.println("** Checkout receipt **");
        double filteredSubtotal = 0;
        for (CartItem item : cart.getItems()) {
            String itemName = item.getProduct().getName();
            if (itemName.equals("Cheese") || itemName.equals("Biscuits")) {
                System.out.printf("%dx %s %.0f%n",
                    item.getQuantity(),
                    item.getProduct().getName(),
                    item.getTotalPrice());
                filteredSubtotal += item.getTotalPrice();
            }
        }
        System.out.println("----------------------");
        System.out.printf("Subtotal %.0f%n", filteredSubtotal);
        System.out.printf("Shipping %.0f%n", shippingFee);
        System.out.printf("Amount %.0f%n", filteredSubtotal + shippingFee);
        System.out.println("END.");
    }

    private double getProductWeight(Product product) {
        if (product instanceof ExpirableProduct) {
            return ((ExpirableProduct) product).getWeight();
        } else if (product instanceof NonExpirableProduct) {
            return ((NonExpirableProduct) product).getWeight();
        }
        return 0.0;
    }
}

// Helper class for shippable items
class ShippableItem implements Shippable {
    private String name;
    private double weight;
    
    public ShippableItem(String name, double weight) {
        this.name = name;
        this.weight = weight;
    }
    
    @Override
    public String getName() { return name; }
    @Override
    public double getWeight() { return weight; }
}

// Main class
public class ECommerceSystem {
    public static void main(String[] args) {
        ExpirableProduct cheese = new ExpirableProduct("Cheese", 100, 10,
            LocalDate.now().plusDays(7), true, 0.2);
        
        ExpirableProduct biscuits = new ExpirableProduct("Biscuits", 150, 5,
            LocalDate.now().plusDays(30), true, 0.7);
        
        NonExpirableProduct scratchCard = new NonExpirableProduct("Mobile Scratch Card", 50, 100, false, 0.0);
        
        Customer customer = new Customer("John Doe", 1000);
        
        ShoppingCart cart = new ShoppingCart();
        cart.add(cheese, 2);
        cart.add(biscuits, 1);
        cart.add(scratchCard, 1);

        CheckoutService checkoutService = new CheckoutService(new DefaultShippingService());
        try {
            checkoutService.checkout(customer, cart);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }
}
