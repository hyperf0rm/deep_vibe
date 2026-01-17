package io.github.hyperf0rm.deep_vibe.music.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tracks",
       uniqueConstraints = { @UniqueConstraint(columnNames = {"name", "artistName"}) })
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String artistName;
    private String previewUrl;
    private boolean isAnalyzed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackQueueStatus status = TrackQueueStatus.NEW;

    private short bpm;
    private float rms;
    private float spectralCentroid;
    // private String key;


    @OneToMany(mappedBy = "track", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Scrobble> scrobbles = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public boolean isAnalyzed() {
        return isAnalyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        isAnalyzed = analyzed;
    }

    public TrackQueueStatus getStatus() {
        return status;
    }

    public void setStatus(TrackQueueStatus status) {
        this.status = status;
    }

    public short getBpm() {
        return bpm;
    }

    public void setBpm(short bpm) {
        this.bpm = bpm;
    }

    public float getRms() {
        return rms;
    }

    public void setRms(float rms) {
        this.rms = rms;
    }

    public float getSpectralCentroid() {
        return spectralCentroid;
    }

    public void setSpectralCentroid(float spectralCentroid) {
        this.spectralCentroid = spectralCentroid;
    }

    public List<Scrobble> getScrobbles() {
        return scrobbles;
    }

    public void setScrobbles(List<Scrobble> scrobbles) {
        this.scrobbles = scrobbles;
    }

    @Override
    public String toString() {
        return this.artistName + " - " + this.name;
    }


}
