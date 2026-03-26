package com.geollm.utils.docx;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocxExtractor {
    // Go 版本正则：
    // sectionRegex := `^第?[一二三四五六七八九十百零〇]+[、.]?`
    // questionRegex := `^\d+[.、．\s]?`
    // choiceRegex := `^[A-D][.．\s]`
    private static final Pattern SECTION = Pattern.compile("^第?[一二三四五六七八九十百零〇]+[、.]?");
    private static final Pattern QUESTION = Pattern.compile("^\\d+[.、．\\s]?");
    private static final Pattern CHOICE = Pattern.compile("^[A-D][.．\\s]");
    private static final Pattern NUMBER_ONLY = Pattern.compile("\\d+");

    public static List<Section> extract(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            List<Section> sections = new ArrayList<>();
            Section currentSection = null;
            Question currentQuestion = null;

            for (XWPFParagraph para : doc.getParagraphs()) {
                StringBuilder sb = new StringBuilder();
                for (XWPFRun run : para.getRuns()) {
                    String t = run.text();
                    if (t != null) {
                        t = t.trim();
                        if (!t.isEmpty()) sb.append(t);
                    }
                }
                String text = sb.toString();
                if (text.isBlank()) continue;

                if (SECTION.matcher(text).find()) {
                    if (currentSection != null) {
                        if (currentQuestion != null) {
                            currentSection.getQuestions().add(currentQuestion);
                            currentQuestion = null;
                        }
                        sections.add(currentSection);
                    }
                    currentSection = new Section();
                    currentSection.setTitle(text);
                    currentSection.setQuestions(new ArrayList<>());
                    continue;
                }

                if (currentSection != null && QUESTION.matcher(text).find()) {
                    if (currentQuestion != null) currentSection.getQuestions().add(currentQuestion);
                    Matcher m = QUESTION.matcher(text);
                    m.find();
                    String qNoRaw = m.group();
                    Matcher n = NUMBER_ONLY.matcher(qNoRaw);
                    n.find();
                    String numberOnly = n.group();

                    String content = text.substring(qNoRaw.length()).trim();
                    currentQuestion = new Question();
                    currentQuestion.setNumber(numberOnly);
                    currentQuestion.setContent(content);
                } else if (currentSection != null && CHOICE.matcher(text).find()) {
                    if (currentQuestion != null) {
                        currentQuestion.setContent(currentQuestion.getContent() + "\n" + text);
                    } else {
                        currentSection.setTitle(currentSection.getTitle() + "\n" + text);
                    }
                } else if (currentSection != null) {
                    if (currentQuestion != null) {
                        currentQuestion.setContent(currentQuestion.getContent() + "\n" + text);
                    } else {
                        currentSection.setTitle(currentSection.getTitle() + "\n" + text);
                    }
                }
            }

            if (currentSection != null) {
                if (currentQuestion != null) currentSection.getQuestions().add(currentQuestion);
                sections.add(currentSection);
            }
            return sections;
        }
    }
}

