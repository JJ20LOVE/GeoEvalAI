package com.geollm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 错题本表
 * </p>
 *
 * @author dhy
 * @since 2026-03-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wrongbook")
public class Wrongbook implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "wrong_id", type = IdType.AUTO)
    private Integer wrongId;

    private Integer studentId;

    private Integer examId;

    private Integer questionId;

    private String questionText;

    private String studentAnswer;

    private String correctAnswer;

    private String analysis;

    private String knowledgePoint;

    private LocalDateTime createTime;


}
