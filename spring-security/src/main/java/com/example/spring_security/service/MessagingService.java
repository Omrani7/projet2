package com.example.spring_security.service;

import com.example.spring_security.dao.ConversationRepository;
import com.example.spring_security.dao.MessageRepository;
import com.example.spring_security.dao.UserRepo;
import com.example.spring_security.dto.ConversationDTO;
import com.example.spring_security.dto.MessageCreateDTO;
import com.example.spring_security.dto.MessageDTO;
import com.example.spring_security.dto.UserBasicDTO;
import com.example.spring_security.exception.ResourceNotFoundException;
import com.example.spring_security.exception.BadRequestException;
import com.example.spring_security.model.Conversation;
import com.example.spring_security.model.Message;
import com.example.spring_security.model.User;
import com.example.spring_security.model.UserPrincipal;
import com.example.spring_security.model.UserProfile;
import com.example.spring_security.websocket.MessagingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing messaging functionality between connected students
 * Handles conversations, messages, and real-time notifications
 */
@Service
@Transactional
public class MessagingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepo userRepository;
    private final ConnectionRequestService connectionRequestService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final MessagingWebSocketHandler messagingWebSocketHandler;
    
    @Autowired
    public MessagingService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          UserRepo userRepository,
                          ConnectionRequestService connectionRequestService,
                          WebSocketNotificationService webSocketNotificationService,
                          MessagingWebSocketHandler messagingWebSocketHandler) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.connectionRequestService = connectionRequestService;
        this.webSocketNotificationService = webSocketNotificationService;
        this.messagingWebSocketHandler = messagingWebSocketHandler;
    }
    
    /**
     * Get all conversations for the current user
     */
    @Transactional(readOnly = true)
    public Page<ConversationDTO> getUserConversations(UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting conversations for user {}", currentUser.getId());
        
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Page<Conversation> conversations = conversationRepository
                .findByParticipantIdOrderByUpdatedAtDesc(currentUser.getId(), pageable);
        
        return conversations.map(conversation -> convertToConversationDTO(conversation, user));
    }
    
    /**
     * Get or create a conversation with another user
     * Only allows messaging between connected students
     */
    public ConversationDTO getOrCreateConversation(Integer otherUserId, UserPrincipal currentUser) {
        logger.info("Getting/creating conversation between user {} and user {}", 
                   currentUser.getId(), otherUserId);
        
        // Validate that users are different
        if (currentUser.getId() == otherUserId) {
            throw new BadRequestException("You cannot create a conversation with yourself");
        }
        
        // Get both users
        User currentUserEntity = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Other user not found"));
        
        // Validate that other user is a student
        if (otherUser.getRole() != User.Role.STUDENT) {
            throw new BadRequestException("You can only message other students");
        }
        
        // Check if users are connected (have accepted connection request)
        if (!connectionRequestService.hasAcceptedConnectionWith(otherUserId, currentUser)) {
            throw new BadRequestException("You can only message users who have accepted your connection request.");
        }
        
        // Check if conversation already exists
        Conversation conversation = conversationRepository
                .findConversationBetweenUsers(currentUser.getId(), otherUserId)
                .orElse(null);
        
        if (conversation == null) {
            // Create new conversation
            conversation = new Conversation();
            Set<User> participants = new HashSet<>();
            participants.add(currentUserEntity);
            participants.add(otherUser);
            conversation.setParticipants(participants);
            
            conversation = conversationRepository.save(conversation);
            logger.info("Created new conversation {} between users {} and {}", 
                       conversation.getId(), currentUser.getId(), otherUserId);
        }
        
        return convertToConversationDTO(conversation, currentUserEntity);
    }
    
    /**
     * Send a message in a conversation
     */
    public MessageDTO sendMessage(MessageCreateDTO messageCreateDTO, UserPrincipal currentUser) {
        logger.info("User {} sending message to conversation {}", 
                   currentUser.getId(), messageCreateDTO.getConversationId());
        
        // Get the conversation
        Conversation conversation = conversationRepository.findByIdWithDetails(messageCreateDTO.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        // Get the sender
        User sender = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        
        // Validate that sender is a participant in the conversation
        if (!conversation.hasParticipantWithId(currentUser.getId())) {
            throw new BadRequestException("You are not a participant in this conversation");
        }
        
        // Create the message
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(messageCreateDTO.getContent().trim())
                .messageType(messageCreateDTO.getMessageType() != null ? 
                           messageCreateDTO.getMessageType() : Message.MessageType.TEXT)
                .metadata(messageCreateDTO.getMetadata())
                .build();
        
        message = messageRepository.save(message);
        
        // Update conversation's updated timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        // Send WebSocket notifications to other participants
        final Message finalMessage = message;
        final User finalSender = sender;
        conversation.getParticipants().stream()
                .filter(participant -> participant.getId() != currentUser.getId())
                .forEach(participant -> {
                    try {
                        // Send via STOMP (existing system)
                        webSocketNotificationService.sendNotification(
                                Long.valueOf(participant.getId()),
                                "NEW_MESSAGE",
                                String.format("New message from %s", finalSender.getUsername()),
                                convertToMessageDTO(finalMessage)
                        );
                        
                        // Also send via simple WebSocket for real-time messaging
                        messagingWebSocketHandler.sendNotificationToUser(
                                participant.getId(),
                                "NEW_MESSAGE",
                                String.format("New message from %s", finalSender.getUsername()),
                                convertToMessageDTO(finalMessage)
                        );
                    } catch (Exception e) {
                        logger.warn("Failed to send WebSocket notification for message {} to user {}: {}", 
                                   finalMessage.getId(), participant.getId(), e.getMessage());
                    }
                });
        
        logger.info("Message {} sent successfully in conversation {}", 
                   message.getId(), messageCreateDTO.getConversationId());
        
        return convertToMessageDTO(message);
    }
    
    /**
     * Get messages for a specific conversation
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> getConversationMessages(Long conversationId, UserPrincipal currentUser, Pageable pageable) {
        logger.debug("Getting messages for conversation {} by user {}", conversationId, currentUser.getId());
        
        // Validate that conversation exists and user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        if (!conversation.hasParticipantWithId(currentUser.getId())) {
            throw new BadRequestException("You are not a participant in this conversation");
        }
        
        // Get messages ordered by timestamp descending (newest first for pagination)
        Page<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampDesc(conversationId, pageable);
        
        return messages.map(this::convertToMessageDTO);
    }
    
    /**
     * Mark messages as read in a conversation
     */
    public void markMessagesAsRead(Long conversationId, UserPrincipal currentUser) {
        logger.debug("Marking messages as read in conversation {} for user {}", 
                    conversationId, currentUser.getId());
        
        // Validate that conversation exists and user is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        
        if (!conversation.hasParticipantWithId(currentUser.getId())) {
            throw new BadRequestException("You are not a participant in this conversation");
        }
        
        // Mark all unread messages as read (excluding user's own messages)
        int updatedCount = messageRepository.markAllMessagesAsReadInConversation(conversationId, currentUser.getId());
        
        logger.debug("Marked {} messages as read in conversation {} for user {}", 
                    updatedCount, conversationId, currentUser.getId());
    }
    
    /**
     * Get unread message count for the current user
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(UserPrincipal currentUser) {
        logger.debug("Getting unread message count for user {}", currentUser.getId());
        
        // Get all conversations for the user and sum up unread counts
        Page<Conversation> conversations = conversationRepository
                .findByParticipantIdOrderByUpdatedAtDesc(currentUser.getId(), Pageable.unpaged());
        
        return conversations.getContent().stream()
                .mapToLong(conversation -> messageRepository
                        .countUnreadMessagesInConversation(conversation.getId(), currentUser.getId()))
                .sum();
    }
    
    /**
     * Convert Conversation entity to ConversationDTO
     */
    private ConversationDTO convertToConversationDTO(Conversation conversation, User currentUser) {
        ConversationDTO dto = ConversationDTO.builder()
                .id(conversation.getId())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .isOneOnOne(conversation.isOneOnOne())
                .build();
        
        // Set other participant for 1-on-1 conversations
        if (conversation.isOneOnOne()) {
            User otherParticipant = conversation.getOtherParticipant(currentUser);
            if (otherParticipant != null) {
                dto.setOtherParticipant(convertUserToBasicDTO(otherParticipant));
            }
        }
        
        // Set last message
        Message latestMessage = conversation.getLatestMessage();
        if (latestMessage != null) {
            dto.setLastMessage(convertToMessageDTO(latestMessage));
        }
        
        // Set unread count
        long unreadCount = messageRepository.countUnreadMessagesInConversation(
                conversation.getId(), currentUser.getId());
        dto.setUnreadCount(unreadCount);
        
        return dto;
    }
    
    /**
     * Convert Message entity to MessageDTO
     */
    private MessageDTO convertToMessageDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .sender(convertUserToBasicDTO(message.getSender()))
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .timestamp(message.getTimestamp())
                .isRead(message.isRead())
                .timeAgo(getTimeAgo(message.getTimestamp()))
                .metadata(message.getMetadata())
                .build();
    }
    
    /**
     * Convert User entity to UserBasicDTO
     */
    private UserBasicDTO convertUserToBasicDTO(User user) {
        UserBasicDTO.UserBasicDTOBuilder builder = UserBasicDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name());
        
        // Add profile information if available
        if (user.getUserProfile() != null) {
            UserProfile profile = user.getUserProfile();
            builder.institute(profile.getInstitute())
                   .fieldOfStudy(profile.getFieldOfStudy())
                   .educationLevel(profile.getEducationLevel() != null ? 
                                 profile.getEducationLevel().name() : null);
        }
        
        return builder.build();
    }
    
    /**
     * Get human-readable time ago string
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days < 7) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }
} 