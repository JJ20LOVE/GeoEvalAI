package com.geollm.mapper;

import com.geollm.dto.xueqing.ExamDataDto;
import com.geollm.dto.xueqing.HistoryDto;
import com.geollm.dto.xueqing.StudentInfoDto;
import com.geollm.dto.xueqing.StudentListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface XueqingMapper {

    @Select("""
            SELECT student.id,
                   student.student_id,
                   student_name,
                   class_name,
                   AVG(total_grade) AS avg_grade,
                   MAX(total_grade) AS max_grade,
                   MIN(total_grade) AS min_grade
            FROM student
            JOIN answersheet ON student.id = answersheet.student_id
            NATURAL JOIN class
            WHERE student.id = #{id}
            GROUP BY student.id
            """)
    StudentInfoDto getStudentInfoBase(Integer id);

    @Select("""
            SELECT answersheet.id,
                   title,
                   exam.create_date AS date,
                   total_grade
            FROM answersheet
            NATURAL JOIN exam
            WHERE student_id = #{id}
            """)
    List<HistoryDto> getStudentHistory(Integer id);

    @Select("""
            SELECT student.id, student.student_id, student_name, total_grade AS grade
            FROM student
            JOIN answersheet ON student.id = answersheet.student_id
            WHERE exam_id = #{examId}
            ORDER BY total_grade DESC
            """)
    List<StudentListDto> getNameList(Integer examId);

    @Select("""
            SELECT MAX(total_grade) AS highest,
                   MIN(total_grade) AS lowest,
                   AVG(total_grade) AS average,
                   COUNT(total_grade) AS count
            FROM answersheet
            WHERE exam_id = #{examId}
            GROUP BY exam_id
            """)
    ExamDataDto getExamData(Integer examId);

    @Select("""
            SELECT AVG(point)
            FROM answersheet_detail
            JOIN answersheet ON aid = answersheet.id
            WHERE exam_id = #{examId}
            GROUP BY qid
            """)
    List<Double> getAvgPointByQid(Integer examId);

    @Select("""
            SELECT point
            FROM exam_detail
            WHERE exam_id = #{examId}
            """)
    List<Double> getFullPointByQid(Integer examId);
}

