package com.sports.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

public class OrderCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "顾客代码不能为空")
    private String customerCode;

    private String shippingRequirement;

    @NotBlank(message = "收件人不能为空")
    private String recipientName;

    @NotBlank(message = "收件地址不能为空")
    private String recipientAddress;

    @NotBlank(message = "收件人电话不能为空")
    private String recipientPhone;

    private String paymentMethod;

    private String remark;

    @NotEmpty(message = "订单细则不能为空")
    @Valid
    private List<OrderItemDTO> items;

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public String getShippingRequirement() {
        return shippingRequirement;
    }

    public void setShippingRequirement(String shippingRequirement) {
        this.shippingRequirement = shippingRequirement;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public static class OrderItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        @NotBlank(message = "商品编码不能为空")
        private String productCode;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
