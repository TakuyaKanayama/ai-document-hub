package com.aidoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI Document Hub - メインアプリケーションクラス
 * Spring Boot アプリケーションのエントリーポイント
 */
@SpringBootApplication
/** JPA監査機能を有効化（作成日時の自動設定など） */
@EnableJpaAuditing
/** モデルクラスのエンティティスキャン */
@EntityScan(basePackages = "com.aidoc.model")
/** JPAリポジトリのスキャン */
@EnableJpaRepositories(basePackages = "com.aidoc.repository")
public class AiDocApplication {

    /**
     * アプリケーションのエントリーポイント
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiDocApplication.class, args);
    }

}
