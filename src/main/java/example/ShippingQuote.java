package example;

record ShippingQuote(int subtotal, int shippingFee) {
    static final int FREE_SHIPPING_THRESHOLD = 40_000;
    static final int STANDARD_SHIPPING_FEE = 3_000;

    static final int EXPRESS_SURCHARGE = 2_000;

    static ShippingQuote forSubtotal(int subtotal) {
        return forSubtotal(subtotal, false);
    }

    static ShippingQuote forSubtotal(int subtotal, boolean express) {
        int shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : STANDARD_SHIPPING_FEE;
        return new ShippingQuote(subtotal, shippingFee + (express ? EXPRESS_SURCHARGE : 0));
    }

    int total() {
        return subtotal + shippingFee;
    }
}
