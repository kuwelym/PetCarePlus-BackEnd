package petitus.petcareplus.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import petitus.petcareplus.model.TermsAndConditions;
import petitus.petcareplus.repository.TermsRepository;
import petitus.petcareplus.utils.enums.TermsType;

@Component
@RequiredArgsConstructor
@Slf4j
public class TermsDataInitializer implements CommandLineRunner {

    private final TermsRepository termsRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeTermsAndConditions();
    }

    private void initializeTermsAndConditions() {
        if (termsRepository.count() > 0) {
            log.info("Terms and conditions already initialized");
            return;
        }

        log.info("Initializing terms and conditions...");

        // User Terms - English
        createUserTermsEn();

        // User Terms - Vietnamese
        createUserTermsVi();

        // Provider Terms - English
        createProviderTermsEn();

        // Provider Terms - Vietnamese
        createProviderTermsVi();

        log.info("Terms and conditions initialized successfully");
    }

    private void createUserTermsEn() {
        String content = """
                # Terms and Conditions for Users

                **Last Updated:** January 2025
                **Version:** 1.0

                ## 1. Introduction

                Welcome to **PetCarePlus**! These Terms and Conditions ("Terms") govern your use of our mobile application and related services (collectively, the "Service"). By accessing or using our Service, you agree to be bound by these Terms.

                ## 2. About Our Service

                PetCarePlus is a platform that connects pet owners with certified pet care service providers. Our Service allows you to:
                - Find and book pet care services
                - Communicate with service providers
                - Make payments for services
                - Rate and review service providers

                ## 3. Payment Terms & Important Notice

                ### 3.1 Payment Processing
                **IMPORTANT NOTICE:** PetCarePlus is currently in development phase and has not yet obtained business registration status. Therefore:

                - **No External Wallet Integration:** We do not currently support external digital wallets (e.g., MoMo, ZaloPay, etc.)
                - **No Bank Account Linking:** Direct bank account integration is not available
                - **QR Code Payments Only:** All payments are processed through QR codes

                ### 3.2 Payment Method
                When you make a payment:
                1. The system will generate a QR code for your payment
                2. You will scan this QR code using your banking app or e-wallet
                3. **The receiving account name will be an individual's name** (not a business name)
                4. This is because we are operating as an individual entity while awaiting business registration

                ### 3.3 Payment Security
                - All payments are processed through secure banking channels
                - We do not store your banking or payment information
                - Payment confirmations are handled by your bank/e-wallet provider

                ### 3.4 Refund Policy
                - Refunds are subject to our refund policy
                - Processing time may vary due to manual verification
                - Refunds will be issued to the original payment method when possible

                ## 4. User Responsibilities

                ### 4.1 Account Security
                - Keep your login credentials secure
                - Do not share your account with others
                - Report any unauthorized access immediately

                ### 4.2 Service Booking
                - Provide accurate information when booking services
                - Communicate clearly with service providers
                - Be present during scheduled service times

                ### 4.3 Payment Obligations
                - Pay for services as agreed
                - Do not dispute legitimate charges
                - Contact us before disputing any charges with your bank

                ## 5. Service Provider Interactions

                ### 5.1 Direct Communication
                - Communication with service providers is direct
                - We facilitate but do not control service quality
                - Any disputes should be reported to our support team

                ### 5.2 Service Quality
                - Service providers are independent contractors
                - We do not guarantee service outcomes
                - We provide a platform for connection only

                ## 6. Data Protection & Privacy

                ### 6.1 Information Collection
                - We collect only necessary information for service provision
                - Your data is protected according to our Privacy Policy
                - We do not sell your personal information

                ### 6.2 Communication Data
                - Messages between users and providers may be monitored for quality assurance
                - We may use communication data to resolve disputes

                ## 7. Prohibited Activities

                You may not:
                - Use the Service for illegal activities
                - Harass or harm other users
                - Attempt to bypass payment systems
                - Submit false or misleading information
                - Violate intellectual property rights

                ## 8. Service Availability

                - Our Service may be unavailable due to maintenance
                - We do not guarantee 100% uptime
                - Critical updates may require temporary service interruption

                ## 9. Limitation of Liability

                **PetCarePlus provides the Service "as is" and makes no warranties.** We are not liable for:
                - Service provider performance
                - Damages to your pet (service provider responsibility)
                - Technical issues beyond our control
                - Third-party payment processing issues

                ## 10. Changes to Terms

                - We may update these Terms periodically
                - Changes will be posted in the app
                - Continued use constitutes acceptance of new Terms

                ## 11. Contact Information

                For questions about these Terms, contact us:
                - **Email:** support@petcareplus.com
                - **Phone:** +84 123 456 789
                - **Address:** Ho Chi Minh City, Vietnam

                ## 12. Governing Law

                These Terms are governed by the laws of Vietnam. Any disputes will be resolved in Vietnamese courts.

                ---

                **By using PetCarePlus, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.**
                """;

        TermsAndConditions userTermsEn = TermsAndConditions.builder()
                .type(TermsType.USER_TERMS)
                .language("en")
                .version("1.0")
                .title("Terms and Conditions for Users")
                .content(content)
                .build();

        termsRepository.save(userTermsEn);
    }

    private void createUserTermsVi() {
        String content = """
                # Điều Khoản Và Điều Kiện Dành Cho Người Dùng

                **Cập nhật lần cuối:** Tháng 1 năm 2025
                **Phiên bản:** 1.0

                ## 1. Giới Thiệu

                Chào mừng bạn đến với **PetCarePlus**! Những Điều khoản và Điều kiện này ("Điều khoản") điều chỉnh việc sử dụng ứng dụng di động và các dịch vụ liên quan của chúng tôi (gọi chung là "Dịch vụ"). Bằng cách truy cập hoặc sử dụng Dịch vụ của chúng tôi, bạn đồng ý bị ràng buộc bởi các Điều khoản này.

                ## 2. Về Dịch Vụ Của Chúng Tôi

                PetCarePlus là một nền tảng kết nối các chủ thú cưng với các nhà cung cấp dịch vụ chăm sóc thú cưng được chứng nhận. Dịch vụ của chúng tôi cho phép bạn:
                - Tìm kiếm và đặt dịch vụ chăm sóc thú cưng
                - Giao tiếp với nhà cung cấp dịch vụ
                - Thanh toán cho dịch vụ
                - Đánh giá và nhận xét về nhà cung cấp dịch vụ

                ## 3. Điều Khoản Thanh Toán & Thông Báo Quan Trọng

                ### 3.1 Xử Lý Thanh Toán
                **THÔNG BÁO QUAN TRỌNG:** PetCarePlus hiện đang trong giai đoạn phát triển và chưa có được tình trạng đăng ký doanh nghiệp. Do đó:

                - **Không Tích Hợp Ví Điện Tử Bên Ngoài:** Chúng tôi hiện không hỗ trợ các ví điện tử bên ngoài (ví dụ: MoMo, ZaloPay, v.v.)
                - **Không Liên Kết Tài Khoản Ngân Hàng:** Tích hợp trực tiếp với tài khoản ngân hàng không khả dụng
                - **Chỉ Thanh Toán QR Code:** Tất cả thanh toán được xử lý thông qua mã QR

                ### 3.2 Phương Thức Thanh Toán
                Khi bạn thực hiện thanh toán:
                1. Hệ thống sẽ tạo mã QR cho khoản thanh toán của bạn
                2. Bạn sẽ quét mã QR này bằng ứng dụng ngân hàng hoặc ví điện tử
                3. **Tên tài khoản nhận sẽ là tên cá nhân** (không phải tên doanh nghiệp)
                4. Điều này do chúng tôi đang hoạt động như một thể nhân trong khi chờ đăng ký doanh nghiệp

                ### 3.3 Bảo Mật Thanh Toán
                - Tất cả thanh toán được xử lý qua các kênh ngân hàng an toàn
                - Chúng tôi không lưu trữ thông tin ngân hàng hoặc thanh toán của bạn
                - Xác nhận thanh toán được xử lý bởi nhà cung cấp ngân hàng/ví điện tử của bạn

                ### 3.4 Chính Sách Hoàn Tiền
                - Hoàn tiền tuân theo chính sách hoàn tiền của chúng tôi
                - Thời gian xử lý có thể khác nhau do xác minh thủ công
                - Hoàn tiền sẽ được thực hiện về phương thức thanh toán gốc khi có thể

                ## 4. Trách Nhiệm Của Người Dùng

                ### 4.1 Bảo Mật Tài Khoản
                - Giữ thông tin đăng nhập an toàn
                - Không chia sẻ tài khoản với người khác
                - Báo cáo ngay lập tức nếu có truy cập trái phép

                ### 4.2 Đặt Dịch Vụ
                - Cung cấp thông tin chính xác khi đặt dịch vụ
                - Giao tiếp rõ ràng với nhà cung cấp dịch vụ
                - Có mặt trong thời gian dịch vụ đã lên lịch

                ### 4.3 Nghĩa Vụ Thanh Toán
                - Thanh toán cho dịch vụ theo thỏa thuận
                - Không tranh chấp các khoản phí hợp lệ
                - Liên hệ với chúng tôi trước khi tranh chấp bất kỳ khoản phí nào với ngân hàng

                ## 5. Tương Tác Với Nhà Cung Cấp Dịch Vụ

                ### 5.1 Giao Tiếp Trực Tiếp
                - Giao tiếp với nhà cung cấp dịch vụ là trực tiếp
                - Chúng tôi tạo điều kiện nhưng không kiểm soát chất lượng dịch vụ
                - Mọi tranh chấp nên được báo cáo cho đội ngũ hỗ trợ của chúng tôi

                ### 5.2 Chất Lượng Dịch Vụ
                - Nhà cung cấp dịch vụ là nhà thầu độc lập
                - Chúng tôi không bảo đảm kết quả dịch vụ
                - Chúng tôi chỉ cung cấp nền tảng để kết nối

                ## 6. Bảo Vệ Dữ Liệu & Quyền Riêng Tư

                ### 6.1 Thu Thập Thông Tin
                - Chúng tôi chỉ thu thập thông tin cần thiết để cung cấp dịch vụ
                - Dữ liệu của bạn được bảo vệ theo Chính sách Quyền riêng tư của chúng tôi
                - Chúng tôi không bán thông tin cá nhân của bạn

                ### 6.2 Dữ Liệu Giao Tiếp
                - Tin nhắn giữa người dùng và nhà cung cấp có thể được giám sát để đảm bảo chất lượng
                - Chúng tôi có thể sử dụng dữ liệu giao tiếp để giải quyết tranh chấp

                ## 7. Hoạt Động Bị Cấm

                Bạn không được:
                - Sử dụng Dịch vụ cho các hoạt động bất hợp pháp
                - Quấy rối hoặc làm hại người dùng khác
                - Cố gắng bỏ qua hệ thống thanh toán
                - Gửi thông tin sai lệch hoặc gây hiểu lầm
                - Vi phạm quyền sở hữu trí tuệ

                ## 8. Tính Khả Dụng Của Dịch Vụ

                - Dịch vụ của chúng tôi có thể không khả dụng do bảo trì
                - Chúng tôi không đảm bảo 100% thời gian hoạt động
                - Các cập nhật quan trọng có thể yêu cầu tạm dừng dịch vụ

                ## 9. Giới Hạn Trách Nhiệm

                **PetCarePlus cung cấp Dịch vụ "như hiện tại" và không đưa ra bảo đảm nào.** Chúng tôi không chịu trách nhiệm cho:
                - Hiệu suất của nhà cung cấp dịch vụ
                - Thiệt hại cho thú cưng của bạn (trách nhiệm của nhà cung cấp dịch vụ)
                - Vấn đề kỹ thuật ngoài tầm kiểm soát của chúng tôi
                - Vấn đề xử lý thanh toán của bên thứ ba

                ## 10. Thay Đổi Điều Khoản

                - Chúng tôi có thể cập nhật các Điều khoản này định kỳ
                - Các thay đổi sẽ được đăng trong ứng dụng
                - Việc tiếp tục sử dụng đồng nghĩa với việc chấp nhận Điều khoản mới

                ## 11. Thông Tin Liên Hệ

                Để có câu hỏi về các Điều khoản này, hãy liên hệ với chúng tôi:
                - **Email:** support@petcareplus.com
                - **Điện thoại:** +84 123 456 789
                - **Địa chỉ:** Thành phố Hồ Chí Minh, Việt Nam

                ## 12. Luật Điều Chỉnh

                Các Điều khoản này được điều chỉnh bởi luật pháp Việt Nam. Mọi tranh chấp sẽ được giải quyết tại các tòa án Việt Nam.

                ---

                **Bằng cách sử dụng PetCarePlus, bạn xác nhận rằng bạn đã đọc, hiểu và đồng ý bị ràng buộc bởi các Điều khoản và Điều kiện này.**
                """;

        TermsAndConditions userTermsVi = TermsAndConditions.builder()
                .type(TermsType.USER_TERMS)
                .language("vi")
                .version("1.0")
                .title("Điều Khoản Và Điều Kiện Dành Cho Người Dùng")
                .content(content)
                .build();

        termsRepository.save(userTermsVi);
    }

    private void createProviderTermsEn() {
        String content = """
                # Terms and Conditions for Service Providers

                **Last Updated:** January 2025
                **Version:** 1.0

                ## 1. Introduction

                Welcome to **PetCarePlus Service Provider Program**! These Terms and Conditions ("Terms") govern your participation as a service provider on our platform. By registering as a service provider, you agree to be bound by these Terms.

                ## 2. Service Provider Requirements

                ### 2.1 Eligibility
                To become a service provider, you must:
                - Be at least 18 years old
                - Have relevant experience in pet care
                - Provide valid identification documents
                - Complete our verification process
                - Maintain good standing on the platform

                ### 2.2 Professional Standards
                - Maintain professional conduct with all clients
                - Provide services as described in your profile
                - Communicate clearly and promptly with clients
                - Follow all applicable laws and regulations

                ## 3. Wallet System & Financial Terms

                ### 3.1 Mandatory Wallet Creation
                **IMPORTANT REQUIREMENT:** All service providers must create and maintain an internal wallet within the PetCarePlus app. This is mandatory for:
                - Receiving payments for services
                - Managing your earnings
                - Requesting withdrawals
                - Tracking financial transactions

                ### 3.2 Payment Processing
                When clients pay for your services:
                1. Payments are processed through our secure system
                2. **Platform fees are automatically deducted** from your earnings
                3. Net earnings are credited to your internal wallet
                4. You can track all transactions in your wallet dashboard

                ### 3.3 Platform Fees
                **PetCarePlus charges the following fees:**
                - **Service Commission:** 5% of each transaction
                - **Withdrawal Fee:** 1% of withdrawal amount (minimum 5,000 VND, maximum 50,000 VND)
                - **Payment Processing Fee:** Covered by the platform

                ### 3.4 Withdrawal System & Important Notice

                **CRITICAL INFORMATION:** Due to our current business status, we operate with limitations:

                #### 3.4.1 No Automatic Withdrawals
                - **We do not offer automatic withdrawal systems**
                - **No direct bank transfers through the app**
                - **No integration with external payment systems**
                - All withdrawals are processed manually by our administration team

                #### 3.4.2 Withdrawal Process
                1. **Submit Withdrawal Request:** Create a withdrawal request through your provider dashboard
                2. **Provide Bank Details:** Include your bank account information (account number, bank name, account holder name)
                3. **Admin Review:** Our admin team will review your request (typically within 24-48 hours)
                4. **Manual Transfer:** If approved, admin will process the bank transfer manually
                5. **Notification:** You will receive notification when the transfer is completed

                #### 3.4.3 Withdrawal Requirements
                - **Minimum Withdrawal:** 50,000 VND
                - **Maximum Withdrawal:** 50,000,000 VND per request
                - **Daily Limit:** 10,000,000 VND
                - **Monthly Limit:** 100,000,000 VND
                - **Processing Time:** 1-3 business days

                #### 3.4.4 Why Manual Processing?
                **Business Registration Status:** PetCarePlus is currently operating as an individual entity and has not yet obtained full business registration. Therefore:
                - We cannot integrate with automated banking systems
                - All financial transactions require manual oversight
                - We are working toward full business registration to provide automated services

                ## 4. Service Responsibilities

                ### 4.1 Service Delivery
                - Provide services as advertised
                - Arrive on time for appointments
                - Maintain professional standards
                - Ensure pet safety and well-being
                - Clean up after services when applicable

                ### 4.2 Communication
                - Respond to client messages within 24 hours
                - Provide clear service descriptions
                - Inform clients of any delays or issues
                - Maintain professional language and tone

                ### 4.3 Documentation
                - Keep records of services provided
                - Take photos/videos when requested by clients
                - Report any incidents or issues immediately
                - Maintain client confidentiality

                ## 5. Wallet Management

                ### 5.1 Balance Monitoring
                - Check your wallet balance regularly
                - Monitor transaction history
                - Report any discrepancies immediately
                - Keep records of all withdrawal requests

                ### 5.2 Withdrawal Best Practices
                - Ensure bank account information is accurate
                - Submit withdrawal requests during business hours
                - Allow adequate processing time
                - Contact support if withdrawals are delayed

                ### 5.3 Fee Transparency
                All fees are clearly displayed:
                - In your service provider dashboard
                - On each transaction record
                - In withdrawal calculations
                - In monthly statements

                ## 6. Platform Policies

                ### 6.1 Quality Standards
                - Maintain a minimum 4.0-star rating
                - Respond to reviews professionally
                - Continuously improve service quality
                - Attend training sessions when required

                ### 6.2 Availability
                - Keep your calendar updated
                - Honor confirmed appointments
                - Provide advance notice for unavailability
                - Maintain reasonable response times

                ## 7. Prohibited Activities

                As a service provider, you may not:
                - Solicit clients outside the platform
                - Share client personal information
                - Provide services while intoxicated
                - Engage in inappropriate behavior
                - Violate animal welfare standards
                - Attempt to circumvent platform fees

                ## 8. Account Suspension & Termination

                ### 8.1 Suspension Reasons
                Your account may be suspended for:
                - Violating these Terms
                - Receiving multiple negative reviews
                - Failing to provide agreed services
                - Inappropriate conduct with clients
                - Fraudulent activity

                ### 8.2 Termination Process
                - We will provide notice when possible
                - Outstanding payments will be processed
                - You may appeal suspension decisions
                - Final wallet balance will be paid out

                ## 9. Financial Disclaimers

                ### 9.1 Earnings Disclaimer
                - **No guaranteed income** from platform participation
                - Earnings depend on service quality and demand
                - Platform fees may change with notice
                - You are responsible for tax obligations

                ### 9.2 Withdrawal Limitations
                - **Manual processing may cause delays**
                - **Admin discretion applies to all withdrawals**
                - **Bank transfer fees may apply** (charged by your bank)
                - **We are not responsible for bank processing delays**

                ## 10. Insurance & Liability

                ### 10.1 Insurance Requirements
                - Service providers are encouraged to maintain liability insurance
                - PetCarePlus does not provide insurance coverage
                - You are responsible for damages caused during service

                ### 10.2 Limitation of Liability
                - Platform facilitates connections only
                - Service providers are independent contractors
                - PetCarePlus is not liable for service outcomes
                - Disputes are between provider and client

                ## 11. Future Platform Updates

                ### 11.1 Automated Systems
                When we achieve full business registration:
                - Automatic withdrawal systems will be implemented
                - Real-time payment processing will be available
                - Enhanced banking integrations will be added
                - Fee structures may be updated

                ### 11.2 Notification of Changes
                - Major updates will be announced in advance
                - Terms may be updated to reflect new capabilities
                - Continued participation constitutes acceptance

                ## 12. Support & Contact

                ### 12.1 Provider Support
                - **Email:** providers@petcareplus.com
                - **Phone:** +84 123 456 789
                - **Support Hours:** Monday-Friday, 9 AM - 6 PM
                - **Emergency Contact:** Available 24/7

                ### 12.2 Financial Support
                For withdrawal or payment issues:
                - Contact our finance team directly
                - Provide transaction details
                - Allow 24-48 hours for response
                - Escalate if issues persist

                ## 13. Legal Compliance

                ### 13.1 Tax Obligations
                - You are responsible for reporting income
                - Keep records of all earnings
                - Consult tax professionals as needed
                - Comply with local tax laws

                ### 13.2 Governing Law
                These Terms are governed by Vietnamese law. All disputes will be resolved in Vietnamese courts.

                ---

                **By becoming a PetCarePlus service provider, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions, including the manual withdrawal process and all associated limitations.**
                """;

        TermsAndConditions providerTermsEn = TermsAndConditions.builder()
                .type(TermsType.PROVIDER_TERMS)
                .language("en")
                .version("1.0")
                .title("Terms and Conditions for Service Providers")
                .content(content)
                .build();

        termsRepository.save(providerTermsEn);
    }

    private void createProviderTermsVi() {
        String content = """
                # Điều Khoản Và Điều Kiện Dành Cho Nhà Cung Cấp Dịch Vụ

                **Cập nhật lần cuối:** Tháng 1 năm 2025
                **Phiên bản:** 1.0

                ## 1. Giới Thiệu

                Chào mừng bạn đến với **Chương Trình Nhà Cung Cấp Dịch Vụ PetCarePlus**! Những Điều khoản và Điều kiện này ("Điều khoản") điều chỉnh việc tham gia của bạn như một nhà cung cấp dịch vụ trên nền tảng của chúng tôi. Bằng cách đăng ký làm nhà cung cấp dịch vụ, bạn đồng ý bị ràng buộc bởi các Điều khoản này.

                ## 2. Yêu Cầu Nhà Cung Cấp Dịch Vụ

                ### 2.1 Điều Kiện Tham Gia
                Để trở thành nhà cung cấp dịch vụ, bạn phải:
                - Ít nhất 18 tuổi
                - Có kinh nghiệm liên quan trong chăm sóc thú cưng
                - Cung cấp giấy tờ tùy thân hợp lệ
                - Hoàn thành quy trình xác minh của chúng tôi
                - Duy trì uy tín tốt trên nền tảng

                ### 2.2 Tiêu Chuẩn Nghề Nghiệp
                - Duy trì hành vi chuyên nghiệp với tất cả khách hàng
                - Cung cấp dịch vụ như mô tả trong hồ sơ của bạn
                - Giao tiếp rõ ràng và kịp thời với khách hàng
                - Tuân thủ tất cả luật pháp và quy định hiện hành

                ## 3. Hệ Thống Ví & Điều Khoản Tài Chính

                ### 3.1 Bắt Buộc Tạo Ví
                **YÊU CẦU QUAN TRỌNG:** Tất cả nhà cung cấp dịch vụ phải tạo và duy trì ví nội bộ trong ứng dụng PetCarePlus. Điều này là bắt buộc để:
                - Nhận thanh toán cho dịch vụ
                - Quản lý thu nhập của bạn
                - Yêu cầu rút tiền
                - Theo dõi giao dịch tài chính

                ### 3.2 Xử Lý Thanh Toán
                Khi khách hàng thanh toán cho dịch vụ của bạn:
                1. Thanh toán được xử lý qua hệ thống bảo mật của chúng tôi
                2. **Phí nền tảng được tự động khấu trừ** từ thu nhập của bạn
                3. Thu nhập ròng được chuyển vào ví nội bộ của bạn
                4. Bạn có thể theo dõi tất cả giao dịch trong bảng điều khiển ví

                ### 3.3 Phí Nền Tảng
                **PetCarePlus tính các khoản phí sau:**
                - **Hoa hồng dịch vụ:** 5% của mỗi giao dịch
                - **Phí rút tiền:** 1% của số tiền rút (tối thiểu 5,000 VND, tối đa 50,000 VND)
                - **Phí xử lý thanh toán:** Được nền tảng chi trả

                ### 3.4 Hệ Thống Rút Tiền & Thông Báo Quan Trọng

                **THÔNG TIN QUAN TRỌNG:** Do tình trạng kinh doanh hiện tại của chúng tôi, chúng tôi hoạt động với những hạn chế:

                #### 3.4.1 Không Có Rút Tiền Tự Động
                - **Chúng tôi không cung cấp hệ thống rút tiền tự động**
                - **Không chuyển khoản ngân hàng trực tiếp qua ứng dụng**
                - **Không tích hợp với hệ thống thanh toán bên ngoài**
                - Tất cả rút tiền được xử lý thủ công bởi đội ngũ quản trị của chúng tôi

                #### 3.4.2 Quy Trình Rút Tiền
                1. **Gửi Yêu Cầu Rút Tiền:** Tạo yêu cầu rút tiền thông qua bảng điều khiển nhà cung cấp
                2. **Cung Cấp Thông Tin Ngân Hàng:** Bao gồm thông tin tài khoản ngân hàng (số tài khoản, tên ngân hàng, tên chủ tài khoản)
                3. **Xem Xét Của Admin:** Đội ngũ admin của chúng tôi sẽ xem xét yêu cầu (thường trong vòng 24-48 giờ)
                4. **Chuyển Khoản Thủ Công:** Nếu được duyệt, admin sẽ xử lý chuyển khoản ngân hàng thủ công
                5. **Thông Báo:** Bạn sẽ nhận được thông báo khi chuyển khoản hoàn tất

                #### 3.4.3 Yêu Cầu Rút Tiền
                - **Số tiền rút tối thiểu:** 50,000 VND
                - **Số tiền rút tối đa:** 50,000,000 VND mỗi yêu cầu
                - **Giới hạn hàng ngày:** 10,000,000 VND
                - **Giới hạn hàng tháng:** 100,000,000 VND
                - **Thời gian xử lý:** 1-3 ngày làm việc

                #### 3.4.4 Tại Sao Xử Lý Thủ Công?
                **Tình Trạng Đăng Ký Kinh Doanh:** PetCarePlus hiện đang hoạt động như một thể nhân và chưa có được đăng ký kinh doanh đầy đủ. Do đó:
                - Chúng tôi không thể tích hợp với hệ thống ngân hàng tự động
                - Tất cả giao dịch tài chính yêu cầu giám sát thủ công
                - Chúng tôi đang nỗ lực hướng tới đăng ký kinh doanh đầy đủ để cung cấp dịch vụ tự động

                ## 4. Trách Nhiệm Dịch Vụ

                ### 4.1 Cung Cấp Dịch Vụ
                - Cung cấp dịch vụ như quảng cáo
                - Đến đúng giờ cho các cuộc hẹn
                - Duy trì tiêu chuẩn chuyên nghiệp
                - Đảm bảo an toàn và sức khỏe cho thú cưng
                - Dọn dẹp sau dịch vụ khi có thể

                ### 4.2 Giao Tiếp
                - Trả lời tin nhắn khách hàng trong vòng 24 giờ
                - Cung cấp mô tả dịch vụ rõ ràng
                - Thông báo cho khách hàng về bất kỳ chậm trễ hoặc vấn đề nào
                - Duy trì ngôn ngữ và giọng điệu chuyên nghiệp

                ### 4.3 Tài Liệu
                - Ghi lại các dịch vụ đã cung cấp
                - Chụp ảnh/video khi khách hàng yêu cầu
                - Báo cáo ngay lập tức bất kỳ sự cố hoặc vấn đề nào
                - Duy trì bảo mật thông tin khách hàng

                ## 5. Quản Lý Ví

                ### 5.1 Giám Sát Số Dư
                - Kiểm tra số dư ví thường xuyên
                - Theo dõi lịch sử giao dịch
                - Báo cáo ngay lập tức bất kỳ sự khác biệt nào
                - Giữ hồ sơ về tất cả yêu cầu rút tiền

                ### 5.2 Thực Hành Tốt Nhất Khi Rút Tiền
                - Đảm bảo thông tin tài khoản ngân hàng chính xác
                - Gửi yêu cầu rút tiền trong giờ làm việc
                - Cho phép thời gian xử lý đầy đủ
                - Liên hệ hỗ trợ nếu rút tiền bị chậm trễ

                ### 5.3 Minh Bạch Phí
                Tất cả phí được hiển thị rõ ràng:
                - Trong bảng điều khiển nhà cung cấp dịch vụ
                - Trên mỗi bản ghi giao dịch
                - Trong tính toán rút tiền
                - Trong báo cáo hàng tháng

                ## 6. Chính Sách Nền Tảng

                ### 6.1 Tiêu Chuẩn Chất Lượng
                - Duy trì đánh giá tối thiểu 4.0 sao
                - Trả lời đánh giá một cách chuyên nghiệp
                - Liên tục cải thiện chất lượng dịch vụ
                - Tham gia các buổi đào tạo khi được yêu cầu

                ### 6.2 Khả Năng Phục Vụ
                - Cập nhật lịch trình của bạn
                - Tôn trọng các cuộc hẹn đã xác nhận
                - Thông báo trước về việc không có mặt
                - Duy trì thời gian phản hồi hợp lý

                ## 7. Hoạt Động Bị Cấm

                Là nhà cung cấp dịch vụ, bạn không được:
                - Tìm kiếm khách hàng bên ngoài nền tảng
                - Chia sẻ thông tin cá nhân của khách hàng
                - Cung cấp dịch vụ khi say rượu
                - Tham gia vào hành vi không phù hợp
                - Vi phạm tiêu chuẩn phúc lợi động vật
                - Cố gắng trốn tránh phí nền tảng

                ## 8. Tạm Dừng & Chấm Dứt Tài Khoản

                ### 8.1 Lý Do Tạm Dừng
                Tài khoản của bạn có thể bị tạm dừng vì:
                - Vi phạm các Điều khoản này
                - Nhận nhiều đánh giá tiêu cực
                - Không cung cấp dịch vụ đã thỏa thuận
                - Hành vi không phù hợp với khách hàng
                - Hoạt động gian lận

                ### 8.2 Quy Trình Chấm Dứt
                - Chúng tôi sẽ thông báo khi có thể
                - Các khoản thanh toán chưa hoàn thành sẽ được xử lý
                - Bạn có thể khiếu nại quyết định tạm dừng
                - Số dư ví cuối cùng sẽ được chi trả

                ## 9. Tuyên Bố Miễn Trừ Tài Chính

                ### 9.1 Tuyên Bố Miễn Trừ Thu Nhập
                - **Không có thu nhập đảm bảo** từ việc tham gia nền tảng
                - Thu nhập phụ thuộc vào chất lượng dịch vụ và nhu cầu
                - Phí nền tảng có thể thay đổi với thông báo
                - Bạn có trách nhiệm với nghĩa vụ thuế

                ### 9.2 Giới Hạn Rút Tiền
                - **Xử lý thủ công có thể gây chậm trễ**
                - **Quyết định của admin áp dụng cho tất cả rút tiền**
                - **Phí chuyển khoản ngân hàng có thể áp dụng** (do ngân hàng của bạn tính)
                - **Chúng tôi không chịu trách nhiệm về chậm trễ xử lý ngân hàng**

                ## 10. Bảo Hiểm & Trách Nhiệm

                ### 10.1 Yêu Cầu Bảo Hiểm
                - Nhà cung cấp dịch vụ được khuyến khích duy trì bảo hiểm trách nhiệm
                - PetCarePlus không cung cấp bảo hiểm
                - Bạn chịu trách nhiệm về thiệt hại gây ra trong quá trình cung cấp dịch vụ

                ### 10.2 Giới Hạn Trách Nhiệm
                - Nền tảng chỉ tạo điều kiện kết nối
                - Nhà cung cấp dịch vụ là nhà thầu độc lập
                - PetCarePlus không chịu trách nhiệm về kết quả dịch vụ
                - Tranh chấp là giữa nhà cung cấp và khách hàng

                ## 11. Cập Nhật Nền Tảng Trong Tương Lai

                ### 11.1 Hệ Thống Tự Động
                Khi chúng tôi đạt được đăng ký kinh doanh đầy đủ:
                - Hệ thống rút tiền tự động sẽ được triển khai
                - Xử lý thanh toán thời gian thực sẽ có sẵn
                - Tích hợp ngân hàng nâng cao sẽ được thêm vào
                - Cấu trúc phí có thể được cập nhật

                ### 11.2 Thông Báo Về Thay Đổi
                - Các cập nhật lớn sẽ được thông báo trước
                - Điều khoản có thể được cập nhật để phản ánh khả năng mới
                - Việc tiếp tục tham gia đồng nghĩa với việc chấp nhận

                ## 12. Hỗ Trợ & Liên Hệ

                ### 12.1 Hỗ Trợ Nhà Cung Cấp
                - **Email:** providers@petcareplus.com
                - **Điện thoại:** +84 123 456 789
                - **Giờ hỗ trợ:** Thứ Hai-Thứ Sáu, 9 AM - 6 PM
                - **Liên hệ khẩn cấp:** Có sẵn 24/7

                ### 12.2 Hỗ Trợ Tài Chính
                Cho các vấn đề rút tiền hoặc thanh toán:
                - Liên hệ trực tiếp với đội ngũ tài chính của chúng tôi
                - Cung cấp chi tiết giao dịch
                - Cho phép 24-48 giờ để phản hồi
                - Báo cáo thêm nếu vấn đề vẫn tồn tại

                ## 13. Tuân Thủ Pháp Luật

                ### 13.1 Nghĩa Vụ Thuế
                - Bạn có trách nhiệm khai báo thu nhập
                - Giữ hồ sơ về tất cả thu nhập
                - Tham khảo ý kiến chuyên gia thuế khi cần
                - Tuân thủ luật thuế địa phương

                ### 13.2 Luật Điều Chỉnh
                Các Điều khoản này được điều chỉnh bởi luật pháp Việt Nam. Tất cả tranh chấp sẽ được giải quyết tại các tòa án Việt Nam.

                ---

                **Bằng cách trở thành nhà cung cấp dịch vụ PetCarePlus, bạn xác nhận rằng bạn đã đọc, hiểu và đồng ý bị ràng buộc bởi các Điều khoản và Điều kiện này, bao gồm quy trình rút tiền thủ công và tất cả các hạn chế liên quan.**
                """;

        TermsAndConditions providerTermsVi = TermsAndConditions.builder()
                .type(TermsType.PROVIDER_TERMS)
                .language("vi")
                .version("1.0")
                .title("Điều Khoản Và Điều Kiện Dành Cho Nhà Cung Cấp Dịch Vụ")
                .content(content)
                .build();

        termsRepository.save(providerTermsVi);
    }
}