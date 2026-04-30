package com.piet.quizhub.service;

import com.piet.quizhub.entity.Question;
import com.piet.quizhub.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; 
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepo; 

    private List<Question> cachedQuestions;

    @PostConstruct
    public void loadQuestions(){
        cachedQuestions = questionRepo.findAll();
        System.out.println("Questions loaded into memory: " + cachedQuestions.size());
    }

    public List<Question> getQuestions(){
        if(cachedQuestions == null) loadQuestions();
        List<Question> shuffled = new ArrayList<>(cachedQuestions);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    public void refreshQuestions(){
        cachedQuestions = questionRepo.findAll();
        System.out.println("Questions cache refreshed: " + cachedQuestions.size());
    }

    public void deleteQuestion(Long id) {
        if(questionRepo.existsById(id)) {
            questionRepo.deleteById(id);
            refreshQuestions(); 
        } else {
            throw new RuntimeException("Question not found with id: " + id);
        }
    }

    public List<Question> getQuestionsByCategory(String category) {
        return questionRepo.findByCategory(category);
    }

    public List<Question> getRandomQuestionsForStudent(String category, int limit) {
        List<Question> allQuestions = questionRepo.findByCategory(category);
        Collections.shuffle(allQuestions);
        return allQuestions.stream()
                           .limit(limit)
                           .collect(Collectors.toList());
    }


    @Transactional 
    public void deleteQuestionsByCategory(String category) {
       
        questionRepo.deleteByCategory(category);
        refreshQuestions();
    }
}