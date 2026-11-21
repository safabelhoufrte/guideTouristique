
package ma.tourisme.guide.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "guide_photo")
public class GuidePhoto {

    @Id
    @Column(name = "guide_id")
    private Long guideId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public GuidePhoto() {}

    public GuidePhoto(Long guideId, String contentType, byte[] data) {
        this.guideId = guideId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getGuideId() {
        return guideId;
    }

    public void setGuideId(Long guideId) {
        this.guideId = guideId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}