package stest.tron.wallet.committee;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


@Slf4j
public class WalletTestCommittee003 {
  private final String testKey002 = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private final String witnessKey001 = "0528dc17428585fc4dece68b79fa7912270a1fe8e85f244372f59eb7e8925e04";
  //Witness 47.93.33.201
  private final String witnessKey002 = "553c7b0dee17d3f5b334925f5a90fe99fb0b93d47073d69ec33eead8459d171e";
  //Witness 123.56.10.6
  private final String witnessKey003 = "324a2052e491e99026442d81df4d2777292840c1b3949e20696c49096c6bacb8";
  //Wtiness 39.107.80.135
  private final String witnessKey004 = "ff5d867c4434ac17d264afc6696e15365832d5e8000f75733ebb336d66df148d";
  //Witness 47.93.184.2
  private final String witnessKey005 = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";


  private final byte[] witness001Address = PublicMethed.getFinalAddress(witnessKey001);
  private final byte[] witness002Address = PublicMethed.getFinalAddress(witnessKey002);
  private final byte[] witness003Address = PublicMethed.getFinalAddress(witnessKey003);
  private final byte[] witness004Address = PublicMethed.getFinalAddress(witnessKey004);
  private final byte[] witness005Address = PublicMethed.getFinalAddress(witnessKey005);


  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private static final long now = System.currentTimeMillis();

  private String fullnode = "47.94.239.172:50051";
  private String soliditynode = "47.94.239.172:50061";

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = true)
  public void testApproveProposal() {
    PublicMethed.sendcoin(witness001Address,1000000L,
        toAddress,testKey003,blockingStubFull);
    PublicMethed.sendcoin(witness002Address,1000000L,
        toAddress,testKey003,blockingStubFull);

    HashMap<Long, Long> proposalMap = new HashMap<Long, Long>();
    proposalMap.put(20L, 1L);
    Assert.assertTrue(PublicMethed.createProposal(witness001Address,witnessKey001,
        proposalMap,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list
    ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    Optional<ProposalList> listProposals =  Optional.ofNullable(proposalList);
    final Integer proposalId = listProposals.get().getProposalsCount();
    logger.info(Integer.toString(proposalId));

    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Get proposal list after approve
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    logger.info(Integer.toString(listProposals.get().getProposals(0).getApprovalsCount()));
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 1);
    //logger.info(Base58.encode58Check(witness002Address));
    //logger.info(Base58.encode58Check(listProposals.get().getProposals(0).
    // getApprovalsList().get(0).toByteArray()));
    Assert.assertTrue(Base58.encode58Check(witness002Address).equals(Base58.encode58Check(
        listProposals.get().getProposals(0).getApprovalsList().get(0).toByteArray())));

    //Failed to approve proposal when you already approval this proposal
    Assert.assertFalse(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));

    //Success to change the option from true to false.
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        false,blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 0);

    //Failed to approvel proposal when you already approval this proposal
    Assert.assertFalse(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        false,blockingStubFull));


    //Non witness can't approval proposal
    Assert.assertFalse(PublicMethed.approveProposal(toAddress,testKey003,proposalId,
        true,blockingStubFull));

    //Muti approval
    Assert.assertTrue(PublicMethed.approveProposal(witness001Address,witnessKey001,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness002Address,witnessKey002,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness003Address,witnessKey003,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness004Address,witnessKey004,proposalId,
        true,blockingStubFull));
    Assert.assertTrue(PublicMethed.approveProposal(witness005Address,witnessKey005,proposalId,
        true,blockingStubFull));
    proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
    listProposals =  Optional.ofNullable(proposalList);
    Assert.assertTrue(listProposals.get().getProposals(0).getApprovalsCount() == 2);


  }
  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


