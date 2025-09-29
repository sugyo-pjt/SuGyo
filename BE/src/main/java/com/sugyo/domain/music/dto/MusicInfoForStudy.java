package com.sugyo.domain.music.dto;

import com.sugyo.domain.music.domain.Music;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MusicInfoForStudy {

    @Schema(description = "노래 id", example = "1")
    private Long musicId;

    @Schema(description = "노래 제목", example = "징글벨")
    private String title;

    @Schema(description = "가수", example = "김진환")
    private String singer;

    @Schema(description = "앨범 사진 url", example = "null")
    private String albumImageUrl;

    @Schema(description = "학습 가능한 단어 개수", example = "17")
    private long countWord;

    public static MusicInfoForStudy from(Music music, long countWord){
        return MusicInfoForStudy.builder()
                .musicId(music.getId())
                .title(music.getTitle())
                .singer(music.getSinger())
                .albumImageUrl(music.getAlbumImageUrl())
                .countWord(countWord)
                .build();
    }
}
