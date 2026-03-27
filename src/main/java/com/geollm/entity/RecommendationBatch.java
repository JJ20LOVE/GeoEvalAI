package com.geollm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("recommendation_batch")
public class RecommendationBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "batch_id", type = IdType.AUTO)
    private Long batchId;

    private Integer studentId;

    private Integer wrongId;

    private String sourceQuestionText;

    private String sourceKnowledgePoint;

    private String modelName;

    private Integer requestLimit;

    private LocalDateTime createTime;
}
