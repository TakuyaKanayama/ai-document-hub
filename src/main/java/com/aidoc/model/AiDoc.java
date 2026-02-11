package com.aidoc.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AIドキュメントエンティティ
 * アップロードされたファイルのメタデータを管理
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
/** JPA監査リスナー（作成日時の自動設定） */
@EntityListeners(AuditingEntityListener.class)
public class AiDoc {

    /** ドキュメントの一意識別子 */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 元のファイル名 */
    @Column(nullable = false)
    private String filename;

    /** MIMEタイプ（例：application/pdf） */
    @Column(nullable = false)
    private String contentType;

    /** ファイルサイズ（バイト） */
    @Column(nullable = false)
    private long size;

    /** ファイルの保存パス */
    @Column(nullable = false)
    private String filePath;

    /** アップロード日時（自動設定） */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /** ベクトルDBへのインデックス完了フラグ */
    private boolean indexed = false;

    /**
     * コンストラクタ
     * @param filename ファイル名
     * @param contentType MIMEタイプ
     * @param size ファイルサイズ
     * @param filePath 保存パス
     */
    public AiDoc(String filename, String contentType, long size, String filePath) {
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.filePath = filePath;
    }
}
