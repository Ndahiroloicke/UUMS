package com.example.uums.service;

import com.example.uums.dto.request.CreateCustomerRequest;
import com.example.uums.dto.request.UpdateCustomerRequest;
import com.example.uums.dto.response.CustomerResponse;
import com.example.uums.entity.Customer;
import com.example.uums.entity.User;
import com.example.uums.enums.CustomerStatus;
import com.example.uums.exception.BusinessRuleException;
import com.example.uums.exception.DuplicateResourceException;
import com.example.uums.exception.ResourceNotFoundException;
import com.example.uums.repository.CustomerRepository;
import com.example.uums.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new DuplicateResourceException("Customer with National ID already exists: " + request.getNationalId());
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer with email already exists: " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .fullNames(request.getFullNames())
                .nationalId(request.getNationalId())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .status(CustomerStatus.ACTIVE)
                .build();

        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
            customer.setUser(user);
        }

        return mapToResponse(customerRepository.save(customer));
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        return mapToResponse(findById(id));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = findById(id);

        if (request.getFullNames() != null) customer.setFullNames(request.getFullNames());
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(customer.getEmail()) &&
                customerRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already in use: " + request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) customer.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());

        return mapToResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse deactivateCustomer(Long id) {
        Customer customer = findById(id);
        customer.setStatus(CustomerStatus.INACTIVE);
        return mapToResponse(customerRepository.save(customer));
    }

    private Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public Customer findEntityById(Long id) {
        return findById(id);
    }

    private CustomerResponse mapToResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullNames(customer.getFullNames())
                .nationalId(customer.getNationalId())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .address(customer.getAddress())
                .status(customer.getStatus())
                .userId(customer.getUser() != null ? customer.getUser().getId() : null)
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
