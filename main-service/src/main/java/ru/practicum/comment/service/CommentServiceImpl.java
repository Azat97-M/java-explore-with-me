package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dal.CommentRepository;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.errorHandler.NotFoundException;
import ru.practicum.event.dal.EventRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.errorHandler.ConflictException;
import ru.practicum.user.dal.UserRepository;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User author = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя оставлять комментарии к неопубликованным событиям");
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .author(author)
                .created(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto) {
        getUserOrThrow(userId);
        Comment comment = getCommentOrThrow(commentId);

        checkCommentAuthor(comment, userId, "изменять");

        comment.setText(newCommentDto.getText());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        getUserOrThrow(userId);
        Comment comment = getCommentOrThrow(commentId);

        checkCommentAuthor(comment, userId, "удалять");

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id " + commentId + " не найден");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        getEventOrThrow(eventId);

        int pageSize = size;
        if (size <= 0) {
            pageSize = 10;
        }
        Pageable pageable = PageRequest.of(from / pageSize, pageSize);

        return commentRepository.findByEventIdOrderByCreatedAsc(eventId, pageable).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден"));
    }

    private void checkCommentAuthor(Comment comment, Long userId, String action) {
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может " + action + " чужой комментарий");
        }
    }
}