package example;

record ShippingQuote(int subtotal, int shippingFee) {
    private static final int FREE_SHIPPING_THRESHOLD = 30_000;
    private static final int STANDARD_SHIPPING_FEE = 3_000;

    static ShippingQuote forSubtotal(int subtotal) {
        int shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : STANDARD_SHIPPING_FEE;
        return new ShippingQuote(subtotal, shippingFee);
    }

    int total() {
        return subtotal + shippingFee;
    }
}
