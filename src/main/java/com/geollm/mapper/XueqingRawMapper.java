package com.geollm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface XueqingRawMapper {

    interface RowQStudentPoint {
        Integer getQid();
        Integer getStudentId();
        String getStudentName();
        Integer getPoint();
    }

    @Select("""
            SELECT qid,
                   answersheet.student_id AS studentId,
                   student_name AS studentName,
                   point
            FROM answersheet_detail
            JOIN answersheet ON answersheet_detail.aid = answersheet.id
            JOIN student ON answersheet.student_id = student.id
            WHERE exam_id = #{examId}
            ORDER BY qid, studentId
            """)
    List<RowQStudentPoint> listQStudentPoints(Integer examId);

    interface RowStudentQPoint {
        Integer getStudentId();
        Integer getQid();
        Integer getPoint();
    }

    @Select("""
            SELECT student_id AS studentId, qid, point
            FROM answersheet_detail
            JOIN answersheet ON answersheet_detail.aid = answersheet.id
            WHERE exam_id = #{examId}
            ORDER BY studentId, qid
            """)
    List<RowStudentQPoint> listStudentQPoints(Integer examId);
}

