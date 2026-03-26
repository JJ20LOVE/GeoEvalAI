package com.geollm.mapper;

import com.geollm.dto.answersheet.AnswerSheetInfoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnswersheetInfoMapper {
    @Select("""
            SELECT qid AS question_id,
                   result AS ocr_result,
                   point,
                   IFNULL(comment,'') AS comment
            FROM answersheet_detail
            NATURAL JOIN ocr
            WHERE aid = #{aid}
            """)
    List<AnswerSheetInfoDto.QuestionInfo> listQuestions(Integer aid);
}

