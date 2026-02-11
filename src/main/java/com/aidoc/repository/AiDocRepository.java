package com.aidoc.repository;

import com.aidoc.model.AiDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ドキュメントエンティティのデータアクセスオブジェクト
 * JPAによるデータベース操作を提供
 */
@Repository
public interface AiDocRepository extends JpaRepository<AiDoc, UUID> {

    /**
     * 全ドキュメントを取得（アップロード日時の降順）
     * @return 新しい順に並べたドキュメントリスト
     */
    List<AiDoc> findAllByOrderByUploadedAtDesc();
}
