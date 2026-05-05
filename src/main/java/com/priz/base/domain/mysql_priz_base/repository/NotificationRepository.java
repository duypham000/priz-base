package com.priz.base.domain.mysql_priz_base.repository;

import com.priz.base.domain.mysql_priz_base.model.NotificationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationModel, String>,
        JpaSpecificationExecutor<NotificationModel> {

    List<NotificationModel> findByStatus(NotificationModel.Status status);

    List<NotificationModel> findByChannelAndStatus(NotificationModel.Channel channel, NotificationModel.Status status);

    List<NotificationModel> findByUserId(String userId);
}
