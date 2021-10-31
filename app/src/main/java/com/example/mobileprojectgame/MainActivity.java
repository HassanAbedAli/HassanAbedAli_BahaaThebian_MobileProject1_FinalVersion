package com.example.mobileprojectgame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    //Variables Declaration

    List<Question> questions; // List that contains Title and Image Url of Apps

    ImageView image,logoEnd,logoStart;

    Button next,startGame,playAgain; //next button is the arrow to move to the next question

    Bitmap imageData;     // holds image data from the url

    TextView result, textScore, time, note, fetchingText, questionNumberText, gameResult,chooseTitle,description;

    RadioGroup radioGroup;
    RadioButton easy,medium,hard; //Radiobuttons for choosing difficulty

    int randomPositionOfCorrectAnswer; // Index of the Correct Answer

    int score;

    int questionNumber=1;

    int difficulty; // 0 = Easy, 1 = Medium , 2 = Hard

    boolean timeOver=false; //used to see if timer is over for answering a specific question
    boolean answered=false; // used to see if the question was answered
    boolean fetchingFinished=false;
    GridLayout grid; //  The grid contains the 4 buttons ( Answers )

    CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        fetchData();
        setContentView(R.layout.activity_main);


        //Image and Grid that contains the 4 buttons ( Answers )
        image=(ImageView) findViewById(R.id.image);
        logoEnd=(ImageView)findViewById(R.id.logoEnd);
        logoStart=(ImageView)findViewById(R.id.logoStart);

        grid=(GridLayout)findViewById(R.id.grid);



        //Score, Timer and Result TextViews
        textScore=(TextView)findViewById(R.id.score);
        time=(TextView)findViewById(R.id.time);
        result=(TextView) findViewById(R.id.result);
        note=(TextView)findViewById(R.id.note);
        fetchingText=(TextView)findViewById(R.id.fetchingText);
        questionNumberText=(TextView)findViewById(R.id.questionNumber);
        gameResult=(TextView)findViewById(R.id.gameResult);
        chooseTitle=(TextView) findViewById(R.id.chooseTitle);
        description = (TextView) findViewById(R.id.description);

        // Initialize radioGroup
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);

        //Initialize Radiobuttons
        easy = (RadioButton) findViewById(R.id.Easy);
        medium = (RadioButton) findViewById(R.id.Medium);
        hard = (RadioButton) findViewById(R.id.Hard);

        // Initialize Buttons
        next = (Button)findViewById(R.id.next);
        startGame=(Button)findViewById(R.id.startGame);
        playAgain=(Button)findViewById(R.id.playAgain);

        startGame.setEnabled(false); //make it unclickable until the user chooses a difficulty
        while(!fetchingFinished){
            //do nothing, just wait and keep button disabled
        }
        fetchingText.setVisibility(View.INVISIBLE); // hide the fetching data textView when finished fetching
        for(int i=0; i<3 ; i++){
            RadioButton radioButton = (RadioButton)radioGroup.getChildAt(i);
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startGame.setEnabled(true); //this is so that when the user chooses a
                    note.setVisibility(View.INVISIBLE);//difficulty, he can start the game
                }
            });
        }

        timer = new CountDownTimer(10000,1000) {
                @Override
                public void onTick(long l) {
                    time.setText("Timer : "+l/1000);
                }

                @Override
                public void onFinish() {
                    result.setTextColor(Color.RED);
                    result.setText("Time finished");
                    changeScore(false);
                    timer.cancel();
                    ColorizeButtons();
                    timeOver=true;
                    answered=true;
                    if(questionNumber==16)
                        gameFinished();
                }
            };

        // the arrow button to move to the next question
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    LoadQuestion();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // the start game button
        startGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startPlay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        playAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAgain();
            }
        });


    }

    public void changeScore(boolean won){
        if(won)
            score+=2;
        else
            score--;

        textScore.setText("Score : "+score);

    }

    public void buttonClick(View view){

        if(questionNumber==16){
            gameFinished();
            return;
        }
    // the !answered is to avoid clicking again on button and getting score after it was answered
        if(!answered){
            if(difficulty<2 || !timeOver){
                if(view.getTag()!="correct"){
                    result.setTextColor(Color.RED);
                    result.setText("Wrong Answer!!");
                    if(difficulty>0)
                        changeScore(false);
                }
                else{
                    result.setTextColor(Color.GREEN);
                    result.setText("Correct!");
                    if(difficulty>0)
                        changeScore(true);
                }

                //Show the result by making it visible
                result.setVisibility(View.VISIBLE);

                //stop the timer after it is answered
                if(difficulty==2)
                    timer.cancel();

                answered=true;
                ColorizeButtons();

            }

        }



    }


    // This is to make the wrong answers Button color RED and the correct one GREEN After click
    public void ColorizeButtons(){
        for(int i=0; i<4; i++){
            if(i!=randomPositionOfCorrectAnswer){
                grid.getChildAt(i).setBackgroundColor(Color.RED);
            }
            else{
                grid.getChildAt(i).setBackgroundColor(Color.GREEN);
            }
        }
        if(answered && questionNumber==15){
            gameFinished();
            return;
        }
        questionNumber++;

    }


    //This method is to fetch the data using a thread so it dont happen on the Main UI thread
    public void fetchData(){

        //create a thread
        ExecutorService executor = Executors.newSingleThreadExecutor();

        //give it the method getQuestions, which gets the questions and puts them in an arrayList<Questions>
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getQuestions();          // this method is implemented below
                    fetchingFinished=true;     // when fetching is finished, indicate that
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // a thread to download the image
    public void LoadImage(String url) throws IOException{
        // we create a new thread to download the image
        new Thread(()->{
            try {
                imageData=getBitmapFromURL(url); //get image Data, this method is implemented below
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(()->{
                image.setImageBitmap(imageData); //set Image to downloaded image on the UI thread
            });

            }).start();
        }


    public Bitmap getBitmapFromURL(String src) throws IOException {

        //image URL
        URL newurl = new URL(src);

        //download image bitmap data
        Bitmap mIcon_val = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());

        //return the iamge data
        return mIcon_val;
    }

    // fetch data from url and put them in list
    public void getQuestions() throws IOException{

        //Web-site URL
        URL apps = new URL("https://www.pcmag.com/picks/best-android-apps");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(apps.openStream()));
        
        String inputLine = "";


        final String regex = "<img .* data-image-loader=\\\"(.*)\\\".* alt=\\\"(.*) Image\\\".*>";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        questions = new ArrayList<Question>();


        while ((inputLine = in.readLine()) != null) { //read line by line and parse it
            Matcher m = pattern.matcher(inputLine);
                    if(m.find()){
                        questions.add(new Question(m.group(2),m.group(1)));
                    }
        }

        in.close();
        System.out.println(questions.size());




    }

    public void LoadQuestion() throws IOException {


        if(!answered && questionNumber==15){
            gameFinished();
            return;
        }


        // In case the user clicked the next arrow to move to the next question, and he
        // did not answer the question, it counts as Wrong
        if(!answered){
            questionNumber++;
            if(difficulty>0)
                changeScore(false);
        }
        questionNumberText.setText("Question: "+questionNumber+"/15");

        //reset answered to false as its a new question
        answered=false;

        //reset timeOver to false as its a new question
        if(difficulty==2)
            timeOver=false;

        // basically make result invisible when we load a new question
        result.setText("");


        // this reset the color of all buttons back to purple after they were GREEN/RED when the
        //user answered the question +
        //It puts all their tags to false so that the index of answer that was correct in the last
        //question does not stay the same in the new question
        for(int i=0; i<4; i++){
        grid.getChildAt(i).setTag("false");
        grid.getChildAt(i).setBackgroundColor(Color.rgb(103,58,183 ));
        }

        //get a Random Question for App
        int randomQuestionChosen = new Random().nextInt(questions.size()-4);

        // Load the Image of the chosen question
        LoadImage(questions.get(randomQuestionChosen).getImageUrl());

        // get the correct answer
        String correctAnswer=questions.get(randomQuestionChosen).getTitle();

        // generate 3 wrong answers
        List<String> wrongAnswers=get3WrongAnswers(randomQuestionChosen);

        //choose a random index to put the correct answer in ( 0 - 3 )
        randomPositionOfCorrectAnswer = new Random().nextInt(4);

        //set correct answer in the randomPosition chosen above and set tag to correct
        Button correct = (Button) grid.getChildAt(randomPositionOfCorrectAnswer);
        correct.setText(correctAnswer);
        correct.setTag("correct");

        // this variable is since the wrong answers list is of size 3, and we iterating over 4 here
        int count =0;
        //pass over all the buttons and put in them wrong answers except the correct button answer
        for(int i=0; i<4; i++){
            if(i!=randomPositionOfCorrectAnswer){
                Button btn = (Button) grid.getChildAt(i);
                btn.setText(wrongAnswers.get(count));
                count++;
            }
        }

        // in case difficulty is hard, start the timer for the question
        if(difficulty==2)
            timer.start();
    }


    public List<String> get3WrongAnswers(int correctAnswerIndex){
        List<String> wrongAnswers = new ArrayList<>();

        List<Integer> alreadyExistIndex = new ArrayList<>();

        for(int i=0; i<3; i++){

            int randomIndex=new Random().nextInt(questions.size()-4);

            //Check if the random generated number not equal to correct answer index
            //+ It dont exist already to avoid duplicate wrong answers
            if(randomIndex!=correctAnswerIndex && !Exist(randomIndex,alreadyExistIndex)){

                wrongAnswers.add(questions.get(randomIndex).getTitle());
                alreadyExistIndex.add(randomIndex);
            }

            else{
                i--;
            }
        }
        return wrongAnswers;
    }

    //Method to check if the number already exist
    public boolean Exist(int num, List<Integer> list){
        for(int i=0; i<list.size(); i++){
            if(num==list.get(i)){
                return true;
            }
        }
        return false;
    }


    public void startPlay() throws IOException {

        chooseTitle.setVisibility(View.INVISIBLE);
        description.setVisibility(View.INVISIBLE);
        radioGroup.setVisibility(View.INVISIBLE);
        startGame.setVisibility(View.INVISIBLE);
        logoStart.setVisibility(View.INVISIBLE);

        grid.setVisibility(View.VISIBLE);
        next.setVisibility(View.VISIBLE);
        image.setVisibility(View.VISIBLE);
        questionNumberText.setVisibility(View.VISIBLE);


        if(easy.isChecked())
            difficulty=0;
        else if(medium.isChecked())
            difficulty=1;
        else if(hard.isChecked())
            difficulty=2;

        if(difficulty>0)
            textScore.setVisibility(View.VISIBLE);
        if(difficulty>1)
            time.setVisibility(View.VISIBLE);
        answered=true;
        LoadQuestion();
        textScore.setText("Score: "+score);

    }

    public void gameFinished(){
        grid.setVisibility(View.INVISIBLE);
        next.setVisibility(View.INVISIBLE);
        image.setVisibility(View.INVISIBLE);
        questionNumberText.setVisibility(View.INVISIBLE);
        textScore.setVisibility(View.INVISIBLE);
        time.setVisibility(View.INVISIBLE);
        result.setVisibility(View.INVISIBLE);

        if(difficulty==0){
            gameResult.setText("Game Finished");
        }
        else{
            if(score>=15)
                gameResult.setText("YOU WON!!");
            else
                gameResult.setText("YOU LOST!!");
        }

        gameResult.setVisibility(View.VISIBLE);
        playAgain.setVisibility(View.VISIBLE);
        logoEnd.setVisibility(View.VISIBLE);

    }

    public void playAgain(){

        chooseTitle.setVisibility(View.VISIBLE);
        description.setVisibility(View.VISIBLE);
        radioGroup.setVisibility(View.VISIBLE);
        startGame.setVisibility(View.VISIBLE);
        logoStart.setVisibility(View.VISIBLE);

        questionNumber=1;
        score=0;
        answered=false;

        gameResult.setVisibility(View.INVISIBLE);
        playAgain.setVisibility(View.INVISIBLE);
        logoEnd.setVisibility(View.INVISIBLE);

    }












}