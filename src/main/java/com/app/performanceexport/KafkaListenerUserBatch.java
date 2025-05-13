package com.app.performanceexport;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaListenerUserBatch {
    private final JdbcTemplate jdbcTemplate;
    public static final int BATCH_SIZE = 5000;

    @KafkaListener(topics = "users-batch", groupId = "user-import-group")
    public void consumeBatch(@Payload byte[] message) {
        try {
            Long start = System.currentTimeMillis();
            UserProto.UserBatch batch = UserProto.UserBatch.parseFrom(message);
            List<UserProto.User> users = batch.getUsersList();
            List<List<UserProto.User>> subBatches = partition(users);
            subBatches.forEach(this::persistBatch);
            Long end = System.currentTimeMillis();
            log.info("Finished processing batch with size {} in {} ms", users.size(), end - start);

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse batch", e);
        }
    }

    private List<List<UserProto.User>> partition(List<UserProto.User> list) {
        List<List<UserProto.User>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            List<UserProto.User> part = list
                    .subList(i, Math.min(i + BATCH_SIZE, list.size()));
            parts.add(part);
        }
        return parts;
    }

    private void persistBatch(List<UserProto.User> batch) {
        String sql = """
                    INSERT INTO public.tbl_user (
                        name, avatar, level, sex, sign, vip_type, vip_status, vip_role,
                        archive, fans, friend, like_num, is_senior
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                UserProto.User u = batch.get(i);
                ps.setString(1, u.getName());
                ps.setString(2, u.getAvatar());
                ps.setObject(3, u.getLevel(), Types.INTEGER);
                ps.setString(4, u.getSex());
                ps.setString(5, u.getSign());
                ps.setObject(6, u.getVipType(), Types.INTEGER);
                ps.setObject(7, u.getVipStatus(), Types.INTEGER);
                ps.setObject(8, u.getVipRole(), Types.INTEGER);
                ps.setObject(9, u.getArchive(), Types.INTEGER);
                ps.setObject(10, u.getFans(), Types.INTEGER);
                ps.setObject(11, u.getFriend(), Types.INTEGER);
                ps.setObject(12, u.getLikeNum(), Types.INTEGER);
                ps.setObject(13, u.getIsSenior(), Types.INTEGER);
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }
}
