/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spotifyplayer;

import com.sun.javafx.collections.ObservableListWrapper;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 *
 * @author bergeron
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    ImageView albumImageView;
    
    @FXML
    TextField input;
    
    @FXML
    Label timeLabel;
    
    @FXML
    Label albumLabel;
    
    @FXML
    Label titleLabel;
    
    @FXML
    Button playButton;
    
    @FXML
    Button leftButton;
    
    @FXML
    Button rightButton;
    
    @FXML
    TableView tracksTableView;
    
    @FXML
    Slider trackSlider;
    
    @FXML
    ProgressIndicator progressIndicator;
    
    @FXML
    Pane pane;

    // Other Fields...
    ScheduledExecutorService sliderExecutor = null;
    MediaPlayer mediaPlayer = null;
    boolean isSliderAnimationActive = false;
    Button lastPlayButtonPressed = null;
    
    //Change to true to see progress indicator
    private BooleanProperty isLoading = new SimpleBooleanProperty(false);

    ArrayList<Album> albums = null;
    int currentAlbumIndex = 0;
    


    private void startMusic(String url) {
        lastPlayButtonPressed.setText("Pause");
        trackSlider.setDisable(false);

        
        if (mediaPlayer != null)
        {
            stopMusic();
        }
        
        mediaPlayer = new MediaPlayer(new Media(url));
        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
            isSliderAnimationActive = true;
            trackSlider.setValue(0);
            trackSlider.setMax(30.0);
            
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.pause();
                mediaPlayer.seek(Duration.ZERO);
                
                isSliderAnimationActive = false;
                trackSlider.setValue(0);
            });
        });
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.submit(new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                while(mediaPlayer.getCurrentTime().compareTo((mediaPlayer.getTotalDuration())) <= 0){
                    timeLabel.setText(mediaPlayer.getCurrentTime().toMinutes() 
                            + ":" + (mediaPlayer.getCurrentTime().toMinutes()%60)
                            + " / " + mediaPlayer.getTotalDuration().toMinutes()
                                    + ":" + (mediaPlayer.getTotalDuration().toSeconds() % 60));
                }
                return null;
            }
        
        });
        
    }
    
    public void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }

    public void playPauseMusic() {
        try
        {
            if (lastPlayButtonPressed != null && lastPlayButtonPressed.getText().equals("Play"))
            {
                lastPlayButtonPressed.setText("Pause");
                
                if (mediaPlayer != null)
                {
                    mediaPlayer.play();
                }
                trackSlider.setValue(mediaPlayer.getCurrentTime().toSeconds());
                isSliderAnimationActive = true;
            }
            else
            {
                lastPlayButtonPressed.setText("Play");
                if (mediaPlayer != null)
                {
                    mediaPlayer.pause();
                }
                isSliderAnimationActive = false;
            }
        }
        catch(Exception e)
        {
            System.err.println("Error playing/pausing song...");
        }        
    }
    
    private void displayAlbum(int albumNumber)
    {
        // TODO - Display all the informations about the album
        //
        //        Artist Name 
        //        Album Name
        //        Album Cover Image
        //        Enable next/previous album buttons, if there is more than one album
        
        
        // Display Tracks for the album passed as parameter
        if (albumNumber >=0 && albumNumber < albums.size())
        {
            currentAlbumIndex = albumNumber;
            Album album = albums.get(albumNumber);
            
            //Must run later or else we get a not on same JavaFX thread exception
            //Took a while to fix, please don't change.
            Platform.runLater(new Runnable(){
                @Override
                public void run() {
                    albumLabel.setText(album.getAlbumName());
                    titleLabel.setText(album.getArtistName());  
                }
            
            });
            String imageURL = album.getImageURL();
            Image coverImage = (imageURL != null) ? new Image(imageURL) : null;
            albumImageView.setImage(coverImage);
            // Set tracks
            ArrayList<TrackForTableView> tracks = new ArrayList<>();
            for (int i=0; i<album.getTracks().size(); ++i)
            {
                TrackForTableView trackForTable = new TrackForTableView();
                Track track = album.getTracks().get(i);
                trackForTable.setTrackNumber(track.getNumber());
                trackForTable.setTrackTitle(track.getTitle());
                trackForTable.setTrackPreviewUrl(track.getUrl());
                tracks.add(trackForTable);
            }
            tracksTableView.setItems(new ObservableListWrapper(tracks));

            trackSlider.setDisable(true);
            trackSlider.setValue(0.0);  
            
        }
    }
    
    private void searchAlbumsFromArtist(String artistName)
    {
        // TODO - Make sure this is not blocking the UI
        currentAlbumIndex = 0;
        String artistId = SpotifyController.getArtistId(artistName);
        albums = SpotifyController.getAlbumDataFromArtist(artistId);        
    }
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        // Setup Table View
        TableColumn<TrackForTableView, Number> trackNumberColumn = new TableColumn("#");
        trackNumberColumn.setCellValueFactory(new PropertyValueFactory("trackNumber"));
        
        TableColumn trackTitleColumn = new TableColumn("Title");
        trackTitleColumn.setCellValueFactory(new PropertyValueFactory("trackTitle"));
        trackTitleColumn.setPrefWidth(250);
        
        TableColumn playColumn = new TableColumn("Preview");
        playColumn.setCellValueFactory(new PropertyValueFactory("trackPreviewUrl"));
        Callback<TableColumn<TrackForTableView, String>, TableCell<TrackForTableView, String>> cellFactory = new Callback<TableColumn<TrackForTableView, String>, TableCell<TrackForTableView, String>>(){
            @Override
            public TableCell<TrackForTableView, String> call(TableColumn<TrackForTableView, String> param) {
                final TableCell<TrackForTableView, String> cell = new TableCell<TrackForTableView, String>(){
                    final Button playButton = new Button("Play");

                    @Override
                    public void updateItem(String item, boolean empty)
                    {
                        if (item != null && item.equals("") == false){
                            playButton.setOnAction(event -> {
                                if (playButton.getText().equals("Pause") || (mediaPlayer != null && mediaPlayer.getMedia().getSource().equals(item)))
                                {
                                    playPauseMusic();
                                }
                                else
                                {
                                    if (lastPlayButtonPressed != null)
                                    {
                                        lastPlayButtonPressed.setText("Play");
                                    }

                                    lastPlayButtonPressed = playButton;
                                    startMusic(item);
                                }
                            });
    
                            setGraphic(playButton);
                        }
                        else{                        
                            setGraphic(null);
                        }

                        setText(null);                        
                    }
                };
                
                return cell;
            }
        };
        playColumn.setCellFactory(cellFactory);
        tracksTableView.getColumns().setAll(trackNumberColumn, trackTitleColumn, playColumn);

        // When slider is released, we must seek in the song
        trackSlider.setOnMouseReleased(new EventHandler() {
            @Override
            public void handle(Event event) {
                if (mediaPlayer != null)
                {
                    mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
                }
            }
        });

        // Schedule the slider to move right every second
        // Set boolean flag to activate/deactivate the animation
        sliderExecutor = Executors.newSingleThreadScheduledExecutor();
        sliderExecutor.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                // We can't update the GUI elements on a separate thread... 
                // Let's call Platform.runLater to do it in main thread!!
                Platform.runLater(new Runnable(){
                    @Override
                    public void run() {
                        // Move slider
                        if (isSliderAnimationActive)
                        {
                            trackSlider.setValue(trackSlider.getValue() + 1.0);
                        }
                    }
                });
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        //Setting the border of the pane
        pane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        
        //Setting the look of the progress indicator
        progressIndicator.visibleProperty().bind(isLoading);
        
        //executorSearch will run the album search asynchronously
        ScheduledExecutorService executorSearch = Executors.newSingleThreadScheduledExecutor();
        //This code will be run whenever the user presses ENTER on the text area
        input.setOnKeyPressed(e -> {
            executorSearch.submit(new Task<Void>(){
                @Override
                protected Void call() throws Exception {
                    if(e.getCode() == KeyCode.ENTER){
                        if("".compareTo(input.getText()) != 0){
                        currentAlbumIndex = 0;
                        search(input.getText());
                        
                    }
                }
                    return null;
                }
            });
        });
        
        //Setting listeners for both album switch buttons (left and right)
        rightButton.setOnAction(e -> {
            try{
                if(currentAlbumIndex != albums.size() - 1 && albums.size() != 0){
                    currentAlbumIndex++;
                }
                displayAlbum(currentAlbumIndex);
            }catch(Exception error){
            }
        });
        
        leftButton.setOnAction(e -> {
            try{
                if(currentAlbumIndex != 0){
                    currentAlbumIndex--;
                }
                displayAlbum(currentAlbumIndex);
            }catch(Exception error){
            }
        });
        
    }
    // This will get called automatically when window is closed
    // See spotifyPlayer.java for details about the setup!
    public void shutdownSlider(){
        if (sliderExecutor != null)
        {
            sliderExecutor.shutdown();
        }
        
        Platform.exit();
    }
    
    public void search(String name){
        isLoading.set(true);
        try{
            searchAlbumsFromArtist(name);
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, "The artist couldn't be found");
        }
        displayAlbum(currentAlbumIndex);
        isLoading.set(false);
    }
}