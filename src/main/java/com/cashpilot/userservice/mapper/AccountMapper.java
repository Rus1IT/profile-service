package com.cashpilot.userservice.mapper;

import com.cashpilot.account.proto.AccountProto;
import com.cashpilot.account.proto.CreateAccountRequest;
import com.cashpilot.account.proto.UpdateAccountRequest;
import com.cashpilot.userservice.entity.Account;
import com.cashpilot.userservice.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import org.mapstruct.MappingConstants;

import java.util.List;


@Mapper(
        componentModel = "spring",
        uses = {TimestampMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AccountMapper {

    @Mapping(source = "userProfile.userId", target = "userProfileId")
    @Mapping(target = "allFields", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    AccountProto entityToProto(Account account);

    List<AccountProto> entitiesToProtos(List<Account> accounts);


    @Mapping(source = "userProfile", target = "userProfile")
    @Mapping(source = "request.bankName", target = "bankName")
    @ValueMapping(source = "BANK_NAME_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
    @Mapping(source = "request.accountName", target = "accountName")
    @Mapping(source = "request.currency", target = "currency")
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "firstTransactionDate", ignore = true)
    @Mapping(target = "lastTransactionDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Account createRequestToEntity(
            CreateAccountRequest request,
            UserProfile userProfile
    );


    @Mapping(source = "accountName", target = "accountName")
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "bankName", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "firstTransactionDate", ignore = true)
    @Mapping(target = "lastTransactionDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(
            UpdateAccountRequest request,
            @MappingTarget Account account
    );
}