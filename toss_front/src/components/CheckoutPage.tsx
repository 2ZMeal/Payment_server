import { useState } from "react";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";

const clientKey = "test_ck_ex6BJGQOVDKaLOgRQWZB3W4w2zNb";

// 백엔드 API 주소 및 테스트용 유저 ID 설정
const BACKEND_URL = "http://localhost:19086/api/v1/payments";
const TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

export function CheckoutPage() {
    const [amount] = useState(50_000);
    const [isLoading, setIsLoading] = useState(false); // 중복 클릭 방지 상태

    const requestPayment = async (tossMethod: "CARD" | "VIRTUAL_ACCOUNT" | "TRANSFER") => {
        if (isLoading) return; // 이미 로딩 중이면 차단
        setIsLoading(true);

        try {
            const backendMethod = tossMethod === "CARD" ? "CARD" : "TOSS";
            const backendPg = tossMethod === "CARD" ? "CARD" : "TOSS";

            // 💡[수정된 부분] 1. 프론트엔드에서 고유한 주문번호(UUID)를 무조건 생성합니다!
            const newOrderId = crypto.randomUUID();

            // 2. 백엔드에 결제 준비 요청 (생성한 orderId를 같이 보냅니다)
            const createResponse = await fetch(BACKEND_URL, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-User-Id": TEST_USER_ID,             // 백엔드 인증 헤더 (ID)
                    "X-User-Roles": "USER",                // ✅ 백엔드 인증 헤더 (권한) 추가
                    "X-User-Email": "test@test.com"        // ✅ 백엔드 인증 헤더 (이메일) 추가
                },
                body: JSON.stringify({
                    orderId: newOrderId,   // 👈 [수정된 부분] 여기에 orderId를 추가했습니다!
                    price: amount,
                    totalPrice: amount,
                    pgProvider: backendPg,
                    paymentMethod: backendMethod,
                }),
            });

            if (!createResponse.ok) {
                const errorData = await createResponse.json();
                alert(`결제 준비 중 오류가 발생했습니다: ${errorData.message}`);
                setIsLoading(false);
                return;
            }

            // 3. 백엔드에 저장이 잘 되었는지 확인 후 주문번호 추출
            const responseJson = await createResponse.json();
            const serverGeneratedOrderId = responseJson.data.orderId;

            console.log("✅ 백엔드 결제 사전 등록(READY) 완료! orderId:", serverGeneratedOrderId);

            // 4. 토스페이먼츠 SDK 호출
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey: ANONYMOUS });

            // 5. 생성했던 orderId로 토스 결제창 오픈
            await payment.requestPayment({
                method: tossMethod as any,
                amount: {
                    currency: "KRW",
                    value: amount,
                },
                orderId: serverGeneratedOrderId,
                orderName: "토스 티셔츠 외 2건",
                customerName: "김토스",
                customerEmail: "customer123@gmail.com",
                successUrl: window.location.origin + "/sandbox/success",
                failUrl: window.location.origin + "/sandbox/fail"
            });

        } catch (error) {
            console.error("Payment Error:", error);
            setIsLoading(false); // 에러 발생 시 로딩 해제
        }
    };

    return (
        <div className="wrapper w-100">
            <div className="max-w-540 w-100">
                <h2 className="title text-center" style={{ marginBottom: "32px", marginTop: "32px" }}>
                    결제 테스트
                </h2>
                <div className="btn-wrapper w-100" style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    <button
                        className="btn primary w-100"
                        onClick={() => requestPayment("CARD")}
                        disabled={isLoading}
                        style={{ opacity: isLoading ? 0.6 : 1, cursor: isLoading ? "not-allowed" : "pointer" }}
                    >
                        {isLoading ? "결제 준비 중..." : "카드 결제하기"}
                    </button>
                    <button
                        className="btn w-100"
                        onClick={() => requestPayment("VIRTUAL_ACCOUNT")}
                        disabled={isLoading}
                        style={{ opacity: isLoading ? 0.6 : 1, cursor: isLoading ? "not-allowed" : "pointer" }}
                    >
                        가상계좌 결제하기
                    </button>
                    <button
                        className="btn w-100"
                        onClick={() => requestPayment("TRANSFER")}
                        disabled={isLoading}
                        style={{ opacity: isLoading ? 0.6 : 1, cursor: isLoading ? "not-allowed" : "pointer" }}
                    >
                        계좌이체 결제하기
                    </button>
                </div>
            </div>
        </div>
    );
}
