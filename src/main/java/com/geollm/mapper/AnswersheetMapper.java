package com.geollm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.geollm.entity.Answersheet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnswersheetMapper extends BaseMapper<Answersheet> {

    interface RowAnswerSheetWithStudentId {
        Integer getId();
        String getStudentId();
        Integer getExamId();
        Integer getTotalGrade();
        Boolean getIsEva();
    }

    @Select("""
            SELECT answersheet.id,
                   student.student_id AS studentId,
                   exam_id AS examId,
                   total_grade AS totalGrade,
                   is_eva AS isEva
            FROM answersheet
            JOIN student ON answersheet.student_id = student.id
            WHERE (answersheet.exam_id = #{examId} OR #{examId} = '')
              AND (student.class_id = #{classId} OR #{classId} = '')
            """)
    List<RowAnswerSheetWithStudentId> listAnswerSheets(String examId, String classId);

    interface RowAnswerSheetBasicInfo {
        Integer getId();
        Integer getStudentId();
        Integer getExamId();
        Integer getTotalGrade();
        Boolean getIsEva();
        String getStudentName();
    }

    @Select("""
            SELECT answersheet.id,
                   answersheet.student_id AS studentId,
                   answersheet.exam_id AS examId,
                   answersheet.total_grade AS totalGrade,
                   answersheet.is_eva AS isEva,
                   student.student_name AS studentName
            FROM answersheet
            JOIN student ON answersheet.student_id = student.id
            WHERE answersheet.id = #{aid}
            """)
    RowAnswerSheetBasicInfo getBasicInfo(Integer aid);

    @Select("SELECT COUNT(*) FROM answersheet WHERE exam_id = #{examId}")
    Integer countByExamId(Integer examId);
}

