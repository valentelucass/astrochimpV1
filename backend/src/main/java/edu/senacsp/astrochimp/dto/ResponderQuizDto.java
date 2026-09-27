package edu.senacsp.astrochimp.dto;

public class ResponderQuizDto {

    private Long alternativaId;

    public ResponderQuizDto() {
    }

    public ResponderQuizDto(Long alternativaId) {
        this.alternativaId = alternativaId;
    }

    public Long getAlternativaId() {
        return alternativaId;
    }

    public void setAlternativaId(Long alternativaId) {
        this.alternativaId = alternativaId;
    }
}
