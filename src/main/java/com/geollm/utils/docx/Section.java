package com.geollm.utils.docx;

import lombok.Data;

import java.util.List;

@Data
public class Section {
    private String Title;
    private List<Question> Questions;
}

