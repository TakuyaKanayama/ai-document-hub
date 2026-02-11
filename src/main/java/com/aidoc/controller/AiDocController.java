package com.aidoc.controller;

import com.aidoc.model.AiDoc;
import com.aidoc.service.AiDocService;
import com.aidoc.service.RagService;
import com.aidoc.exception.RagException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * ドキュメント関連のHTTPリクエストを処理するコントローラ
 * ファイルのアップロード、削除、質問応答などのエンドポイントを提供
 */
@Controller
public class AiDocController {

    /** ドキュメント操作サービス */
    private final AiDocService aiDocService;
    /** RAG（検索拡張生成）サービス */
    private final RagService ragService;

    /**
     * コンストラクタ
     * @param documentService ドキュメントサービス
     * @param ragService RAGサービス
     */
    public AiDocController(AiDocService aiDocService, RagService ragService) {
        this.aiDocService = aiDocService;
        this.ragService = ragService;
    }

    /**
     * ホーム画面表示
     * アップロードされたドキュメント一覧を取得して表示
     * @param model ビューに渡すデータ
     * @return テンプレート名
     */
    @GetMapping("/")
    public String index(Model model) {
        List<AiDoc> documents = aiDocService.getAllDocuments();
        model.addAttribute("documents", documents);
        return "index";
    }

    /**
     * ファイルアップロード処理
     * アップロードされたファイルを保存し、ベクトルストアに登録
     * @param file アップロードされたファイル
     * @param redirectAttributes リダイレクト時に渡す属性
     * @return リダイレクト先
     */
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            aiDocService.store(file);
            redirectAttributes.addFlashAttribute("message",
                    file.getOriginalFilename() + " をアップロードしました！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    file.getOriginalFilename() + " のアップロードに失敗しました => " + e.getMessage());
        }

        return "redirect:/";
    }

    /**
     * ドキュメント削除処理
     * ドキュメントをデータベース、ファイルシステム、ベクトルストアから削除
     * @param id 削除するドキュメントのID
     * @param redirectAttributes リダイレクト時に渡す属性
     * @return リダイレクト先
     */
    @PostMapping("/delete")
    public String deleteDocument(@RequestParam("id") UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            aiDocService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("message",
                    "ドキュメントを削除しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "ドキュメントの削除に失敗しました: " + e.getMessage());
        }

        return "redirect:/";
    }

    /**
     * 質問応答処理
     * アップロードされたドキュメントに基づいてAIが質問に回答
     * @param message ユーザーの質問
     * @param model ビューに渡すデータ
     * @return HTMXフラグメントまたはテンプレート
     */
    @PostMapping("/ask")
    public String ask(@RequestParam("message") String message, Model model, HttpHeaders headers) {
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        try {
            String answer = ragService.generateAnswer(message);
            model.addAttribute("question", message);
            model.addAttribute("answer", answer);
            model.addAttribute("isError", false);
        } catch (RagException e) {
            // ユーザー向けエラーメッセージ
            model.addAttribute("question", message);
            model.addAttribute("answer", e.getUserFriendlyMessage());
            model.addAttribute("isError", true);
        } catch (Exception e) {
            // 予期せぬエラーの一般エラーメッセージ
            model.addAttribute("question", message);
            model.addAttribute("answer", "申し訳ございません。エラーが発生しました。もう一度お試しください。");
            model.addAttribute("isError", true);
        }
        return "fragments/chat-message :: message"; // HTMXスワップ用のフラグメントを作成
    }
}
