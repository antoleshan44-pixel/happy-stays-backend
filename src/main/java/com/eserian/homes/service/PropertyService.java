package com.eserian.homes.service;

import com.eserian.homes.model.Property;
import com.eserian.homes.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    // ============================================
    // BASIC METHODS (No media eager loading)
    // ============================================

    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    public List<Property> getApprovedProperties() {
        return propertyRepository.findByStatus("APPROVED");
    }

    public List<Property> getPropertiesByOwnerId(Long ownerId) {
        return propertyRepository.findByOwnerId(ownerId);
    }

    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id).orElse(null);
    }

    // ============================================
    // NEW METHODS WITH MEDIA (Photos & Videos)
    // ============================================

    /**
     * Get pending properties with photos and videos eagerly loaded
     */
    @Transactional(readOnly = true)
    public List<Property> getPendingPropertiesWithMedia() {
        return propertyRepository.findPendingPropertiesWithMedia("PENDING");
    }

    /**
     * Get a single property with its photos and videos
     */
    @Transactional(readOnly = true)
    public Property getPropertyWithMediaById(Long id) {
        return propertyRepository.findPropertyWithMediaById(id);
    }

    /**
     * Get owner's properties with photos and videos
     */
    @Transactional(readOnly = true)
    public List<Property> getOwnerPropertiesWithMedia(Long ownerId) {
        return propertyRepository.findPropertiesByOwnerWithMedia(ownerId);
    }

    /**
     * Get all properties with media (for admin)
     */
    @Transactional(readOnly = true)
    public List<Property> getAllPropertiesWithMedia() {
        return propertyRepository.findAllPropertiesWithMedia();
    }

    // ============================================
    // CRUD METHODS
    // ============================================

    public Property createProperty(Property property) {
        if (property.getCreatedAt() == null) {
            property.setCreatedAt(LocalDateTime.now());
        }
        if (property.getUpdatedAt() == null) {
            property.setUpdatedAt(LocalDateTime.now());
        }
        if (property.getStatus() == null) {
            property.setStatus("PENDING");
        }
        return propertyRepository.save(property);
    }

    public Property updateProperty(Long id, Property propertyDetails) {
        Property property = propertyRepository.findById(id).orElse(null);
        if (property != null) {
            property.setTitle(propertyDetails.getTitle());
            property.setDescription(propertyDetails.getDescription());
            property.setLocation(propertyDetails.getLocation());
            property.setPricePerNight(propertyDetails.getPricePerNight());
            property.setBedrooms(propertyDetails.getBedrooms());
            property.setBathrooms(propertyDetails.getBathrooms());
            property.setImageUrl(propertyDetails.getImageUrl());
            property.setStatus(propertyDetails.getStatus());
            property.setAmenities(propertyDetails.getAmenities());
            property.setPropertyType(propertyDetails.getPropertyType());
            property.setUpdatedAt(LocalDateTime.now());
            return propertyRepository.save(property);
        }
        return null;
    }

    public boolean deleteProperty(Long id) {
        if (propertyRepository.existsById(id)) {
            propertyRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Approve a property
     */
    public Property approveProperty(Long id, String moderatorNotes, Long approvedBy) {
        Property property = propertyRepository.findById(id).orElse(null);
        if (property != null) {
            property.setStatus("APPROVED");
            property.setApprovedAt(LocalDateTime.now());
            property.setApprovedBy(approvedBy);
            property.setModeratorNotes(moderatorNotes);
            property.setUpdatedAt(LocalDateTime.now());
            return propertyRepository.save(property);
        }
        return null;
    }

    /**
     * Reject a property
     */
    public Property rejectProperty(Long id, String reason, String details, Boolean allowResubmission) {
        Property property = propertyRepository.findById(id).orElse(null);
        if (property != null) {
            property.setStatus("REJECTED");
            property.setRejectionReason(reason);
            property.setRejectionDetails(details);
            property.setAllowResubmission(allowResubmission != null ? allowResubmission : true);
            property.setUpdatedAt(LocalDateTime.now());
            return propertyRepository.save(property);
        }
        return null;
    }
}