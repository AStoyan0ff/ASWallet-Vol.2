package STARTER.DTOs;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentItemDTO {

    private final String receiverUser;
    private final String avatarImageSrc;
    private final boolean hasCustomAvatar;
    private final String initials;
    private final int avatarTone;
    private final String data;
    private final String dateLabel;
}
