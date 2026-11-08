package vn.cozynest.identity.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import vn.cozynest.common.dto.ApiResponse;

import java.io.IOException;

@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String uri = request.getRequestURI();
        ApiResponse<Void> apiResponse;

        if (uri != null && uri.contains("/landlord") && !uri.contains("/kyc")) {
            apiResponse = ApiResponse.error("Tài khoản chưa được phê duyệt quyền Chủ trọ. Vui lòng hoàn tất xác minh KYC.", "ERR_LANDLORD_NOT_APPROVED");
        } else {
            apiResponse = ApiResponse.error("Bạn không có quyền thực hiện hành động này", "ERR_AUTH_FORBIDDEN");
        }

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
